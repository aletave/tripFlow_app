package com.tripflow.booking.data.service;

import com.stripe.model.PaymentIntent;
import com.tripflow.booking.data.dao.PagamentoRepository;
import com.tripflow.booking.data.dao.PrenotazioneRepository;
import com.tripflow.booking.data.dto.responses.PagamentoIntentResponse;
import com.tripflow.booking.data.dto.responses.PagamentoResponse;
import com.tripflow.booking.data.entities.Pagamento;
import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.entities.enums.StatoPagamento;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import com.tripflow.booking.data.service.PagamentoService;
import com.tripflow.booking.data.service.events.PagamentoCompletatoEvent;
import com.tripflow.booking.data.service.events.PrenotazioneAnnullataEvent;
import com.tripflow.booking.mapper.PagamentoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tripflow.booking.exception.PrenotazioneNotFoundException;
import com.tripflow.booking.exception.PagamentoNotFoundException;
import com.tripflow.booking.exception.PagamentoEsistenteException;
import com.tripflow.booking.exception.StatoPrenotazioneException;
import com.tripflow.booking.exception.StatoPagamentoException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// Implementazione di PagamentoService

//per ora ho solo preparato stripe, ma per ora solo mock,
//implementerò stripe come si deve successivamente


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PagamentoServiceImpl implements PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StripeService stripeService;


    @Override
    public PagamentoIntentResponse avviaPagamento(UUID prenotazioneId,
                                                  UUID viaggiatoreId) {

        // 1. Carico la prenotazione
        Prenotazione prenotazione = prenotazioneRepository.findById(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        // 2. Check ownership
        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Avvio pagamento negato: prenotazione {} richiesta da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        // 3. Check stato prenotazione
        if (prenotazione.getStato() != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile avviare pagamento: prenotazione in stato " + prenotazione.getStato());
        }

        // 4. Check pagamento già esistente
        Optional<Pagamento> trova_esistente = pagamentoRepository.findByPrenotazioneId(prenotazioneId);
        if (trova_esistente.isPresent()) {
            throw new PagamentoEsistenteException(
                    "Pagamento già esistente per la prenotazione " + prenotazioneId);
        }

        // 5. Crea il PaymentIntent VERO su Stripe.
        //    L'importo è SEMPRE il prezzoTotale della prenotazione, mai dal client.
        PaymentIntent paymentIntent =
                stripeService.creaPaymentIntent(prenotazione.getPrezzoTotale(), prenotazioneId);

        // 6. Persisto il pagamento con l'id reale del PaymentIntent.
        //    metodo resta null: lo popoleremo dal webhook quando il pagamento è COMPLETATO.
        Pagamento pagamento = Pagamento.builder()
                .prenotazione(prenotazione)
                .importo(prenotazione.getPrezzoTotale())
                .stato(StatoPagamento.IN_ATTESA)
                .stripePaymentIntentId(paymentIntent.getId())
                .build();

        Pagamento saved = pagamentoRepository.save(pagamento);

        log.info("Pagamento avviato: id={}, prenotazione={}, importo={}, paymentIntent={}",
                saved.getId(), prenotazioneId, saved.getImporto(), paymentIntent.getId());

        // 7. Restituisco il client_secret all'app Android per la PaymentSheet.
        return PagamentoIntentResponse.builder()
                .pagamentoId(saved.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .importo(saved.getImporto())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagamentoResponse trovaPagamento(UUID prenotazioneId, UUID viaggiatoreId) {

        Pagamento pagamento = pagamentoRepository.findByPrenotazioneId(prenotazioneId)
                .orElseThrow(() -> new PagamentoNotFoundException(
                        "Nessun pagamento trovato per la prenotazione " + prenotazioneId));

        UUID ownerId = pagamento.getPrenotazione().getViaggiatoreId();
        if (!ownerId.equals(viaggiatoreId)) {
            log.warn("Accesso negato a pagamento per prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, ownerId);
            throw new AccessDeniedException("Pagamento non accessibile");
        }

        return PagamentoMapper.toResponse(pagamento);
    }

    @Override
    public PagamentoResponse confermaPagamento(String stripePaymentIntentId) {

        Pagamento pagamento = pagamentoRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PagamentoNotFoundException(
                        "Nessun pagamento trovato con paymentIntentId " + stripePaymentIntentId));

        StatoPagamento statoAttuale = pagamento.getStato();

        // check webhook duplicati.
        if (statoAttuale == StatoPagamento.COMPLETATO) {
            log.info("Pagamento {} già COMPLETATO, ignoro la conferma duplicata", pagamento.getId());
            return PagamentoMapper.toResponse(pagamento);
        }

        if (statoAttuale != StatoPagamento.IN_ATTESA) {
            throw new StatoPagamentoException(
                    "Impossibile confermare: pagamento in stato " + statoAttuale);
        }

        StripeService.DettagliCarta carta = stripeService.recuperaDettagliCarta(stripePaymentIntentId);

        pagamento.setStato(StatoPagamento.COMPLETATO);
        pagamento.setDataPagamento(LocalDateTime.now());
        pagamento.setBrandCarta(carta.brand());
        pagamento.setUltimeQuattroCifre(carta.ultime4());
        pagamento.setMetodo(carta.metodo());

        Pagamento saved = pagamentoRepository.save(pagamento);

        UUID prenotazioneId = pagamento.getPrenotazione().getId();
        log.info("Pagamento {} confermato per prenotazione {}", saved.getId(), prenotazioneId);


        eventPublisher.publishEvent(new PagamentoCompletatoEvent(prenotazioneId));

        return PagamentoMapper.toResponse(saved);
    }

    @Override
    public PagamentoResponse gestisciPagamentoFallito(String stripePaymentIntentId) {

        Pagamento pagamento = pagamentoRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PagamentoNotFoundException(
                        "Nessun pagamento trovato con paymentIntentId " + stripePaymentIntentId));

        if (pagamento.getStato() == StatoPagamento.FALLITO) {
            log.info("Pagamento {} già FALLITO, ignoro la notifica duplicata", pagamento.getId());
            return PagamentoMapper.toResponse(pagamento);
        }

        pagamento.setStato(StatoPagamento.FALLITO);
        Pagamento saved = pagamentoRepository.save(pagamento);

        log.warn("Pagamento {} fallito per prenotazione {}",
                saved.getId(), pagamento.getPrenotazione().getId());

        // La prenotazione resta IN_ATTESA: il viaggiatore può ritentare il pagamento.

        return PagamentoMapper.toResponse(saved);
    }

    @Override
    public PagamentoResponse rimborsaPagamento(UUID prenotazioneId) {

        Pagamento pagamento = pagamentoRepository.findByPrenotazioneId(prenotazioneId)
                .orElseThrow(() -> new PagamentoNotFoundException(
                        "Nessun pagamento trovato per la prenotazione " + prenotazioneId));

        // Solo i pagamenti completati possono essere rimborsati.
        // (Se il pagamento è IN_ATTESA o FALLITO, non c'è nulla da rimborsare.)
        if (pagamento.getStato() != StatoPagamento.COMPLETATO) {
            throw new StatoPagamentoException(
                    "Impossibile rimborsare: pagamento in stato " + pagamento.getStato());
        }

        // TODO: chiamare Stripe per il rimborso vero
        pagamento.setStato(StatoPagamento.RIMBORSATO);
        Pagamento saved = pagamentoRepository.save(pagamento);

        log.info("Pagamento {} rimborsato per prenotazione {}", saved.getId(), prenotazioneId);

        return PagamentoMapper.toResponse(saved);
    }

    //Listener degli eventi di annullamento prenotazione.
    @EventListener
    public void onPrenotazioneAnnullata(PrenotazioneAnnullataEvent event) {
        if (!event.eraConfermata()) {
            log.debug("Prenotazione {} annullata ma non era pagata, niente da rimborsare",
                    event.prenotazioneId());
            return;
        }

        log.debug("Ricevuto evento PrenotazioneAnnullata (eraConfermata) per prenotazione {}",
                event.prenotazioneId());

        rimborsaPagamento(event.prenotazioneId());
    }
}