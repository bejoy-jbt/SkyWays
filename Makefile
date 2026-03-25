.PHONY: up down build clean logs ps admin-user

up:
	docker compose up --build -d

down:
	docker compose down

build:
	docker compose build

clean:
	docker compose down -v --remove-orphans

logs:
	docker compose logs -f

ps:
	docker compose ps

# Create admin user in auth DB
admin-user:
	docker exec -i postgres-auth psql -U authuser -d authdb < infra/create-admin.sql
	@echo "Admin created: admin@flightapp.com / Admin@123"

# Open service logs
log-booking:
	docker compose logs -f booking-service

log-payment:
	docker compose logs -f payment-service

log-notif:
	docker compose logs -f notification-service

# Open shells
shell-auth-db:
	docker exec -it postgres-auth psql -U authuser -d authdb

shell-flight-db:
	docker exec -it postgres-flight psql -U flightuser -d flightdb

shell-booking-db:
	docker exec -it postgres-booking psql -U bookinguser -d bookingdb
