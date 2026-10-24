package com.tripflow.booking.data.service;

import com.stripe.model.PaymentIntent;
import com.tripflow.booking.client.UserAuthClient;
import com.tripflow.booking.client.dto.BirthdayCouponDTO;
import com.tripflow.booking.config.PrenotazioneProperties;
import com.tripflow.booking.config.ScontoProperties;
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
import com.tripflow.booking.exception.CodiceScontoNonValidoException;
import com.tripflow.booking.exception.ServizioNonDisponibileException;
import feign.FeignException;
import org.springframework.security.access.AccessDeniedException;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PagamentoServiceImpl implements PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StripeService stripeService;
    private final PrenotazioneProperties prenotazioneProperties;
    private final ScontoProperties scontoProperties;
    private final UserAuthClient userAuthClient;


    @Override
    public PagamentoIntentResponse avviaPagamento(UUID prenotazioneId,
                                                  UUID viaggiatoreId,
                                                  String codiceSconto) {

        Prenotazione prenotazione = prenotazioneRepository.findById(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Avvio pagamento negato: prenotazione {} richiesta da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }


        if (prenotazione.getStato() != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile avviare pagamento: prenotazione in stato " + prenotazione.getStato());
        }

        if (prenotazione.isScadutaAl(LocalDateTime.now())) {
            throw new StatoPrenotazioneException(
                    "Impossibile avviare pagamento: la prenotazione " + prenotazioneId +
                            " e' scaduta il " + prenotazione.getScadenzaIl());
        }

        ImportoDaPagare importo = calcolaImporto(prenotazione, viaggiatoreId, codiceSconto);

        Optional<Pagamento> trova_esistente = pagamentoRepository.findByPrenotazioneId(prenotazioneId);
        if (trova_esistente.isPresent()) {
            Pagamento esistente = trova_esistente.get();
            boolean riprendibile = esistente.getStato() == StatoPagamento.IN_ATTESA
                    || esistente.getStato() == StatoPagamento.FALLITO;
            if (!riprendibile) {
                throw new PagamentoEsistenteException(
                        "Pagamento già esistente per la prenotazione " + prenotazioneId);
            }
            PaymentIntent intentEsistente = stripeService.recuperaPaymentIntent(esistente.getStripePaymentIntentId());
            if (!"canceled".equals(intentEsistente.getStatus())) {
                return riprendiPagamento(esistente, intentEsistente, importo);
            }
        }

        LocalDateTime nuovaScadenza = LocalDateTime.now().plus(prenotazioneProperties.finestraPagamento());
        if(nuovaScadenza.isAfter(prenotazione.getScadenzaIl())) {
            prenotazione.setScadenzaIl(nuovaScadenza);
        }

        PaymentIntent paymentIntent =
                stripeService.creaPaymentIntent(importo.finale(), prenotazioneId, importo.codiceSconto());


        Pagamento pagamento;
        if (trova_esistente.isPresent()) {
            pagamento = trova_esistente.get();
            pagamento.setStato(StatoPagamento.IN_ATTESA);
            pagamento.setStripePaymentIntentId(paymentIntent.getId());
            log.info("Ritento pagamento fallito {} per prenotazione {}",
                    pagamento.getId(), prenotazioneId);
        } else {
            pagamento = Pagamento.builder()
                    .prenotazione(prenotazione)
                    .stato(StatoPagamento.IN_ATTESA)
                    .stripePaymentIntentId(paymentIntent.getId())
                    .build();
        }
        applicaImporto(pagamento, importo);

        Pagamento saved = pagamentoRepository.save(pagamento);

        log.info("Pagamento avviato: id={}, prenotazione={}, importo={}, codiceSconto={}, paymentIntent={}",
                saved.getId(), prenotazioneId, saved.getImporto(), saved.getCodiceSconto(), paymentIntent.getId());

        return costruisciIntentResponse(saved, paymentIntent);
    }

    private PagamentoIntentResponse riprendiPagamento(Pagamento pagamento, PaymentIntent paymentIntent,
                                                      ImportoDaPagare importo) {
        String statoStripe = paymentIntent.getStatus();
        boolean daCompletare = "requires_payment_method".equals(statoStripe)
                || "requires_action".equals(statoStripe);
        if (!daCompletare) {
            throw new PagamentoEsistenteException(
                    "Pagamento già in elaborazione per la prenotazione " + pagamento.getPrenotazione().getId());
        }

        boolean importoCambiato = pagamento.getImporto().compareTo(importo.finale()) != 0;
        if (importoCambiato) {
            if (!"requires_payment_method".equals(statoStripe)) {
                throw new PagamentoEsistenteException(
                        "Pagamento già in corso per la prenotazione " + pagamento.getPrenotazione().getId()
                                + ": impossibile modificare il codice sconto");
            }
            paymentIntent = stripeService.aggiornaImportoPaymentIntent(
                    paymentIntent.getId(), importo.finale(), importo.codiceSconto());
            log.info("Aggiornato importo del pagamento {} da {} a {} (codiceSconto={})",
                    pagamento.getId(), pagamento.getImporto(), importo.finale(), importo.codiceSconto());
        }

        applicaImporto(pagamento, importo);
        if (pagamento.getStato() == StatoPagamento.FALLITO) {
            pagamento.setStato(StatoPagamento.IN_ATTESA);
        }
        Pagamento saved = pagamentoRepository.save(pagamento);

        log.info("Ripreso pagamento {} con PaymentIntent {}", saved.getId(), paymentIntent.getId());

        return costruisciIntentResponse(saved, paymentIntent);
    }

    private ImportoDaPagare calcolaImporto(Prenotazione prenotazione, UUID viaggiatoreId, String codiceSconto) {
        BigDecimal prezzoPieno = prenotazione.getPrezzoTotale();
        if (codiceSconto == null || codiceSconto.isBlank()) {
            return new ImportoDaPagare(prezzoPieno, null, null);
        }

        String codiceInserito = codiceSconto.trim();

        BirthdayCouponDTO couponCompleanno = null;
        if (codiceInserito.equalsIgnoreCase(scontoProperties.codiceCompleanno())) {
            couponCompleanno = verificaCodiceCompleanno(viaggiatoreId);
        }

        StripeService.CodiceSconto sconto = stripeService.recuperaCodiceSconto(codiceInserito);

        if (couponCompleanno != null) {
            boolean giaUsatoQuestoCompleanno = pagamentoRepository.codiceGiaUsatoDaViaggiatoreNelPeriodo(
                    sconto.codice(), viaggiatoreId, prenotazione.getId(), LocalDateTime.now(),
                    couponCompleanno.getValidFrom().atStartOfDay(),
                    couponCompleanno.getValidUntil().plusDays(1).atStartOfDay());
            if (giaUsatoQuestoCompleanno) {
                log.warn("Codice compleanno {} già utilizzato dal viaggiatore {} nel periodo {} - {}",
                        sconto.codice(), viaggiatoreId,
                        couponCompleanno.getValidFrom(), couponCompleanno.getValidUntil());
                throw new CodiceScontoNonValidoException(
                        "Hai già utilizzato il codice sconto " + sconto.codice() + " per questo compleanno");
            }
        } else {
            boolean giaUsato = pagamentoRepository.codiceGiaUsatoDaViaggiatore(
                    sconto.codice(), viaggiatoreId, prenotazione.getId(), LocalDateTime.now());
            if (giaUsato) {
                log.warn("Codice sconto {} già utilizzato dal viaggiatore {}", sconto.codice(), viaggiatoreId);
                throw new CodiceScontoNonValidoException("Hai già utilizzato il codice sconto " + sconto.codice());
            }
        }

        BigDecimal riduzione = prezzoPieno
                .multiply(sconto.percentuale())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finale = prezzoPieno.subtract(riduzione);

        log.info("Codice sconto {} ({}%) applicato alla prenotazione {}: {} -> {}",
                sconto.codice(), sconto.percentuale(), prenotazione.getId(), prezzoPieno, finale);

        return new ImportoDaPagare(finale, prezzoPieno, sconto.codice());
    }

    private BirthdayCouponDTO verificaCodiceCompleanno(UUID viaggiatoreId) {
        BirthdayCouponDTO coupon;
        try {
            coupon = userAuthClient.getBirthdayCoupon();
        } catch (FeignException e) {
            log.error("Errore comunicazione con user-auth-service per il codice compleanno del viaggiatore {}",
                    viaggiatoreId, e);
            throw new ServizioNonDisponibileException(
                    "Impossibile verificare il codice compleanno: user-auth-service non disponibile");
        }

        if (!coupon.isEligible()) {
            log.warn("Codice compleanno rifiutato per il viaggiatore {}: {}", viaggiatoreId, coupon.getReason());
            String messaggio = coupon.getMessage();
            if (messaggio == null || messaggio.isBlank()) {
                messaggio = "Codice compleanno non utilizzabile";
            }
            throw new CodiceScontoNonValidoException(messaggio);
        }

        if (coupon.getValidFrom() == null || coupon.getValidUntil() == null) {
            log.error("Risposta user-auth-service senza periodo di validità per il viaggiatore {}", viaggiatoreId);
            throw new ServizioNonDisponibileException(
                    "Impossibile verificare il codice compleanno: risposta di user-auth-service incompleta");
        }

        return coupon;
    }

    private void applicaImporto(Pagamento pagamento, ImportoDaPagare importo) {
        pagamento.setImporto(importo.finale());
        pagamento.setImportoOriginale(importo.originale());
        pagamento.setCodiceSconto(importo.codiceSconto());
    }

    private PagamentoIntentResponse costruisciIntentResponse(Pagamento pagamento, PaymentIntent paymentIntent) {
        return PagamentoIntentResponse.builder()
                .pagamentoId(pagamento.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .importo(pagamento.getImporto())
                .importoOriginale(pagamento.getImportoOriginale())
                .codiceSconto(pagamento.getCodiceSconto())
                .publishableKey(stripeService.getPublishableKey())
                .build();
    }

    private record ImportoDaPagare(BigDecimal finale, BigDecimal originale, String codiceSconto) {
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

        // check webhook duplicati
        if (statoAttuale == StatoPagamento.COMPLETATO) {
            log.info("Pagamento {} già COMPLETATO, ignoro la conferma duplicata", pagamento.getId());
            return PagamentoMapper.toResponse(pagamento);
        }

        if (statoAttuale != StatoPagamento.IN_ATTESA && statoAttuale != StatoPagamento.FALLITO) {
            throw new StatoPagamentoException(
                    "Impossibile confermare: pagamento in stato " + statoAttuale);
        }

        StripeService.DettagliCarta carta = stripeService.recuperaDettagliCarta(stripePaymentIntentId);

        pagamento.setDataPagamento(LocalDateTime.now());
        pagamento.setBrandCarta(carta.brand());
        pagamento.setUltimeQuattroCifre(carta.ultime4());
        pagamento.setMetodo(carta.metodo());

        Prenotazione prenotazione = pagamento.getPrenotazione();
        boolean prenotazioneAncoraValida = prenotazione.getStato() == StatoPrenotazione.IN_ATTESA
                && !prenotazione.isScadutaAl(LocalDateTime.now());

        if (!prenotazioneAncoraValida) {
            pagamento.setStato(StatoPagamento.RIMBORSATO);
            Pagamento saved = pagamentoRepository.save(pagamento);
            log.warn("Pagamento {} arrivato con prenotazione {} in stato {} (scadenza {}): rimborsato",
                    saved.getId(), prenotazione.getId(), prenotazione.getStato(), prenotazione.getScadenzaIl());
            return PagamentoMapper.toResponse(saved);
        }

        pagamento.setStato(StatoPagamento.COMPLETATO);
        Pagamento saved = pagamentoRepository.save(pagamento);

        log.info("Pagamento {} confermato per prenotazione {}", saved.getId(), prenotazione.getId());
        eventPublisher.publishEvent(new PagamentoCompletatoEvent(prenotazione.getId()));
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

        return PagamentoMapper.toResponse(saved);
    }

    @Override
    public PagamentoResponse rimborsaPagamento(UUID prenotazioneId) {

        Pagamento pagamento = pagamentoRepository.findByPrenotazioneId(prenotazioneId)
                .orElseThrow(() -> new PagamentoNotFoundException(
                        "Nessun pagamento trovato per la prenotazione " + prenotazioneId));

        //Solo i pagamenti completati possono essere rimborsati
        if (pagamento.getStato() != StatoPagamento.COMPLETATO) {
            throw new StatoPagamentoException(
                    "Impossibile rimborsare: pagamento in stato " + pagamento.getStato());
        }

        //Rimborso  solo a livello applicativo: non chiamo Stripe per questo
        pagamento.setStato(StatoPagamento.RIMBORSATO);
        Pagamento saved = pagamentoRepository.save(pagamento);

        log.info("Pagamento {} rimborsato per prenotazione {}", saved.getId(), prenotazioneId);

        return PagamentoMapper.toResponse(saved);
    }

    //Listener degli eventi di annullamento prenotazione
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