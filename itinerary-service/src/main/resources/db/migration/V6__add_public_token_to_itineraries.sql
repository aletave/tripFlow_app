ALTER TABLE itineraries
    ADD COLUMN public_token UUID;

ALTER TABLE itineraries
    ADD CONSTRAINT uk_itineraries_public_token UNIQUE (public_token);
