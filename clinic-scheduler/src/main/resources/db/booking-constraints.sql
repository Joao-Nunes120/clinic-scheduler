BEGIN;

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- O fim de uma reserva deve ser posterior ao início.
ALTER TABLE bookings
ADD CONSTRAINT valid_booking_interval
CHECK (end_time > start_time);

-- Impede reservas sobrepostas para a mesma sala.
ALTER TABLE bookings
ADD CONSTRAINT no_room_overlap
EXCLUDE USING gist (
    room_id WITH =,
    tsrange(start_time, end_time, '[)') WITH &&
);

-- Impede reservas sobrepostas para o mesmo profissional.
ALTER TABLE bookings
ADD CONSTRAINT no_professional_overlap
EXCLUDE USING gist (
    professional_id WITH =,
    tsrange(start_time, end_time, '[)') WITH &&
);

COMMIT;