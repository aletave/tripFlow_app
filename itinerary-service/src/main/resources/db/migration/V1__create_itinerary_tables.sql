CREATE TABLE itineraries (
                             id BIGSERIAL PRIMARY KEY,

                             owner_id BIGINT NOT NULL,

                             title VARCHAR(150) NOT NULL,
                             description TEXT,

                             visibility VARCHAR(30) NOT NULL,

                             start_date DATE,
                             end_date DATE,

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE itinerary_stops (
                                 id BIGSERIAL PRIMARY KEY,

                                 itinerary_id BIGINT NOT NULL,

                                 stop_order INTEGER NOT NULL,

                                 stop_type VARCHAR(30) NOT NULL,

                                 viaggio_id UUID,
                                 attivita_id UUID,

                                 custom_title VARCHAR(150),
                                 custom_description TEXT,

                                 start_datetime TIMESTAMP,
                                 end_datetime TIMESTAMP,

                                 notes TEXT,

                                 CONSTRAINT fk_itinerary_stops_itinerary
                                     FOREIGN KEY (itinerary_id)
                                         REFERENCES itineraries(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT unique_itinerary_stop_order
                                     UNIQUE (itinerary_id, stop_order),

                                 CONSTRAINT chk_stop_type
                                     CHECK (stop_type IN ('VIAGGIO', 'ATTIVITA')),

                                 CONSTRAINT chk_stop_reference
                                     CHECK (
                                         (stop_type = 'VIAGGIO' AND viaggio_id IS NOT NULL AND attivita_id IS NULL AND custom_title IS NULL)
                                             OR
                                         (stop_type = 'ATTIVITA' AND attivita_id IS NOT NULL AND viaggio_id IS NULL AND custom_title IS NULL)
                                         )
);