$ErrorActionPreference = "Stop"

$sql = @"
BEGIN;

DELETE FROM bookings
WHERE client_email LIKE 'teste-%@example.com';

SELECT COUNT(*) AS reservas_restantes
FROM bookings;

COMMIT;
"@

docker exec clinic-postgres psql `
    -U clinic `
    -d clinic_scheduler `
    -v ON_ERROR_STOP=1 `
    -c $sql

if ($LASTEXITCODE -ne 0) {
    throw "A limpeza das reservas de teste falhou."
}