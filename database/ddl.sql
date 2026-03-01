-- DROP SCHEMA public;

CREATE SCHEMA public AUTHORIZATION pg_database_owner;

COMMENT ON SCHEMA public IS 'standard public schema';

-- DROP TYPE public."account_payable_status_enum";

CREATE TYPE public."account_payable_status_enum" AS ENUM (
	'PENDING',
	'PAID',
	'REJECTED');

-- DROP TYPE public."address_type";

CREATE TYPE public."address_type" AS ENUM (
	'punto de encuentro',
	'punto de finalizacion',
	'punto de recogida');

-- DROP TYPE public."age_price_type_enum";

CREATE TYPE public."age_price_type_enum" AS ENUM (
	'ADULT',
	'CHILD',
	'INFANT');

-- DROP TYPE public."duration_type_enum";

CREATE TYPE public."duration_type_enum" AS ENUM (
	'HORAS',
	'DIAS');

-- DROP TYPE public."inclusion_type";

CREATE TYPE public."inclusion_type" AS ENUM (
	'incluido',
	'no incluido');

-- DROP TYPE public."person_type";

CREATE TYPE public."person_type" AS ENUM (
	'adulto',
	'niño',
	'bebé');

-- DROP TYPE public."reservation_status";

CREATE TYPE public."reservation_status" AS ENUM (
	'PENDING',
	'CONFIRMED',
	'CANCELED',
	'COMPLETED');

-- DROP TYPE public."review_reason";

CREATE TYPE public."review_reason" AS ENUM (
	'Excelente experiencia',
	'Buena organización',
	'Guía profesional',
	'No cumplió expectativas',
	'Problemas logísticos',
	'Otro');

-- DROP TYPE public."shopping_cart_status_enum";

CREATE TYPE public."shopping_cart_status_enum" AS ENUM (
	'ACTIVE',
	'PAID',
	'COMPLETED',
	'ABANDONED');

-- DROP TYPE public."tour_duration_type_enum";

CREATE TYPE public."tour_duration_type_enum" AS ENUM (
	'HORAS',
	'DIAS');

-- DROP TYPE public."tour_tag_category_enum";

CREATE TYPE public."tour_tag_category_enum" AS ENUM (
	'Actividades acuáticas',
	'Naturaleza y paisajes',
	'Playas y relax',
	'Aventura y emoción',
	'Cultura y tradición',
	'Comida y bebida',
	'Para familias y niños',
	'Experiencias románticas',
	'Momentos del día',
	'Logística y comodidad',
	'Nocturnos');

-- DROP SEQUENCE public._user_id_seq;

CREATE SEQUENCE public._user_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.account_payable_id_seq;

CREATE SEQUENCE public.account_payable_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.app_config_id_seq;

CREATE SEQUENCE public.app_config_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.category_id_seq;

CREATE SEQUENCE public.category_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.city_id_seq;

CREATE SEQUENCE public.city_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.country_id_seq;

CREATE SEQUENCE public.country_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.credit_id_seq;

CREATE SEQUENCE public.credit_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.maritim_activity_report_id_seq;

CREATE SEQUENCE public.maritim_activity_report_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.payment_payment_id_seq;

CREATE SEQUENCE public.payment_payment_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.provider_id_seq;

CREATE SEQUENCE public.provider_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.request_provider_document_type_id_seq;

CREATE SEQUENCE public.request_provider_document_type_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.request_provider_gallery_id_seq;

CREATE SEQUENCE public.request_provider_gallery_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.request_provider_id_seq;

CREATE SEQUENCE public.request_provider_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.reservation_reservation_id_seq;

CREATE SEQUENCE public.reservation_reservation_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.review_answer_answer_id_seq;

CREATE SEQUENCE public.review_answer_answer_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.review_answer_attachment_id_seq;

CREATE SEQUENCE public.review_answer_attachment_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.review_attachment_id_seq;

CREATE SEQUENCE public.review_attachment_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.review_id_seq;

CREATE SEQUENCE public.review_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.role_id_seq;

CREATE SEQUENCE public.role_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.service_id_seq;

CREATE SEQUENCE public.service_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.service_type_id_seq;

CREATE SEQUENCE public.service_type_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.shopping_cart_id_seq;

CREATE SEQUENCE public.shopping_cart_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.shopping_cart_item_detail_id_seq;

CREATE SEQUENCE public.shopping_cart_item_detail_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.shopping_cart_item_id_seq;

CREATE SEQUENCE public.shopping_cart_item_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.state_id_seq;

CREATE SEQUENCE public.state_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.token_id_seq;

CREATE SEQUENCE public.token_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_address_id_seq;

CREATE SEQUENCE public.tour_address_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_cancel_category_id_seq;

CREATE SEQUENCE public.tour_cancel_category_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_cancellation_policy_id_seq;

CREATE SEQUENCE public.tour_cancellation_policy_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_category_id_seq;

CREATE SEQUENCE public.tour_category_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_faq_id_seq;

CREATE SEQUENCE public.tour_faq_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_gallery_id_seq;

CREATE SEQUENCE public.tour_gallery_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_id_seq;

CREATE SEQUENCE public.tour_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_includes_excludes_id_seq;

CREATE SEQUENCE public.tour_includes_excludes_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_itinerary_id_seq;

CREATE SEQUENCE public.tour_itinerary_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_main_attractions_id_seq;

CREATE SEQUENCE public.tour_main_attractions_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_price_id_seq;

CREATE SEQUENCE public.tour_price_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_reservation_detail_id_seq;

CREATE SEQUENCE public.tour_reservation_detail_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_reservation_id_seq;

CREATE SEQUENCE public.tour_reservation_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_reservation_status_history_id_seq;

CREATE SEQUENCE public.tour_reservation_status_history_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_review_id_seq;

CREATE SEQUENCE public.tour_review_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_review_image_id_seq;

CREATE SEQUENCE public.tour_review_image_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_schedule_config_id_seq;

CREATE SEQUENCE public.tour_schedule_config_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_schedule_config_price_id_seq;

CREATE SEQUENCE public.tour_schedule_config_price_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_schedule_config_slot_id_seq;

CREATE SEQUENCE public.tour_schedule_config_slot_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_schedule_id_seq;

CREATE SEQUENCE public.tour_schedule_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_schedule_status_history_id_seq;

CREATE SEQUENCE public.tour_schedule_status_history_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_status_history_id_seq;

CREATE SEQUENCE public.tour_status_history_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;
-- DROP SEQUENCE public.tour_tag_id_seq;

CREATE SEQUENCE public.tour_tag_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;-- public._user definition

-- Drop table

-- DROP TABLE public._user;

CREATE TABLE public._user (
	id serial4 NOT NULL,
	account_locked bool NOT NULL,
	created_date timestamp NOT NULL,
	date_of_birth date NULL,
	email varchar(50) NULL,
	enabled bool NOT NULL,
	firstname varchar(50) NULL,
	last_modified_date timestamp NULL,
	lastname varchar(50) NULL,
	"password" varchar(255) NULL,
	uuid_social varchar(255) NULL,
	CONSTRAINT _user_pkey PRIMARY KEY (id),
	CONSTRAINT user_email_unique UNIQUE (email)
);


-- public.category definition

-- Drop table

-- DROP TABLE public.category;

CREATE TABLE public.category (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	CONSTRAINT category_pkey PRIMARY KEY (id)
);


-- public.country definition

-- Drop table

-- DROP TABLE public.country;

CREATE TABLE public.country (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT country_name_key UNIQUE (name),
	CONSTRAINT country_pkey PRIMARY KEY (id)
);


-- public.payment definition

-- Drop table

-- DROP TABLE public.payment;

CREATE TABLE public.payment (
	payment_id bigserial NOT NULL,
	transaction_id varchar(255) NOT NULL,
	transaction_data text NULL,
	payer_id int4 NOT NULL,
	payer_name varchar(255) NOT NULL,
	payer_email varchar(255) NOT NULL,
	payer_phone varchar(20) NULL,
	payer_document_type varchar(50) NULL,
	payer_document_number varchar(50) NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT payment_pkey PRIMARY KEY (payment_id)
);
CREATE INDEX idx_payment_payer_email ON public.payment USING btree (payer_email);
CREATE INDEX idx_payment_payer_id ON public.payment USING btree (payer_id);
CREATE INDEX idx_payment_transaction_id ON public.payment USING btree (transaction_id);


-- public.request_provider_document_type definition

-- Drop table

-- DROP TABLE public.request_provider_document_type;

CREATE TABLE public.request_provider_document_type (
	id serial4 NOT NULL,
	"name" varchar(30) NOT NULL,
	mandatory bool DEFAULT false NOT NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	description varchar(255) NULL,
	CONSTRAINT request_provider_document_type_pkey PRIMARY KEY (id),
	CONSTRAINT uq_request_provider_document_type_name UNIQUE (name)
);


-- public."role" definition

-- Drop table

-- DROP TABLE public."role";

CREATE TABLE public."role" (
	id serial4 NOT NULL,
	created_date timestamp NOT NULL,
	last_modified_date timestamp NULL,
	"name" varchar(50) NULL,
	CONSTRAINT role_name_unique UNIQUE (name),
	CONSTRAINT role_pkey PRIMARY KEY (id)
);


-- public.service_type definition

-- Drop table

-- DROP TABLE public.service_type;

CREATE TABLE public.service_type (
	id serial4 NOT NULL,
	"name" varchar(100) NOT NULL,
	description varchar(500) NULL,
	is_active bool DEFAULT true NOT NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT service_type_pkey PRIMARY KEY (id)
);


-- public.shopping_cart_item_detail definition

-- Drop table

-- DROP TABLE public.shopping_cart_item_detail;

CREATE TABLE public.shopping_cart_item_detail (
	id bigserial NOT NULL,
	shopping_cart_item_id int8 NOT NULL,
	age_type varchar(20) NOT NULL,
	quantity int4 NOT NULL,
	unit_price numeric(10, 2) NOT NULL,
	provider_unit_price numeric(10, 2) NULL,
	total_price numeric(10, 2) NOT NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by varchar(255) NULL,
	last_modified_by varchar(255) NULL,
	CONSTRAINT shopping_cart_item_detail_pkey PRIMARY KEY (id)
);


-- public.tour_cancel_category definition

-- Drop table

-- DROP TABLE public.tour_cancel_category;

CREATE TABLE public.tour_cancel_category (
	id serial4 NOT NULL,
	"name" varchar(50) NOT NULL,
	description text NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT tour_cancel_category_name_key UNIQUE (name),
	CONSTRAINT tour_cancel_category_pkey PRIMARY KEY (id)
);


-- public.tour_category definition

-- Drop table

-- DROP TABLE public.tour_category;

CREATE TABLE public.tour_category (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	description varchar(30) NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT tour_category_name_key UNIQUE (name),
	CONSTRAINT tour_category_pkey PRIMARY KEY (id)
);


-- public.tour_tag definition

-- Drop table

-- DROP TABLE public.tour_tag;

CREATE TABLE public.tour_tag (
	id serial4 NOT NULL,
	category public."tour_tag_category_enum" NOT NULL,
	"name" varchar(150) NOT NULL,
	description text NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT tour_tag_category_name_key UNIQUE (category, name),
	CONSTRAINT tour_tag_pkey PRIMARY KEY (id)
);


-- public.app_config definition

-- Drop table

-- DROP TABLE public.app_config;

CREATE TABLE public.app_config (
	id bigserial NOT NULL,
	config_key varchar(100) NOT NULL, -- Clave única de la configuración (ej: CANCELLATION_POLICY)
	config_value jsonb NOT NULL, -- Valor de la configuración en formato JSONB
	description text NULL, -- Descripción de la configuración
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT app_config_config_key_key UNIQUE (config_key),
	CONSTRAINT app_config_pkey PRIMARY KEY (id),
	CONSTRAINT fk_app_config_created_by FOREIGN KEY (created_by) REFERENCES public._user(id),
	CONSTRAINT fk_app_config_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES public._user(id)
);
CREATE INDEX idx_app_config_key ON public.app_config USING btree (config_key);
COMMENT ON TABLE public.app_config IS 'Tabla de configuración del sistema en formato clave-valor';

-- Column comments

COMMENT ON COLUMN public.app_config.config_key IS 'Clave única de la configuración (ej: CANCELLATION_POLICY)';
COMMENT ON COLUMN public.app_config.config_value IS 'Valor de la configuración en formato JSONB';
COMMENT ON COLUMN public.app_config.description IS 'Descripción de la configuración';


-- public.maritim_activity_report definition

-- Drop table

-- DROP TABLE public.maritim_activity_report;

CREATE TABLE public.maritim_activity_report (
	id bigserial NOT NULL,
	country varchar(100) NOT NULL, -- País donde se realiza la actividad
	city varchar(100) NOT NULL, -- Ciudad donde se realiza la actividad
	activity varchar(255) NOT NULL, -- Nombre de la actividad marítima (ej: TOUR BAHIA, KITE SURFING, etc.)
	flag varchar(20) NOT NULL, -- Bandera de alerta: GREEN, YELLOW, RED
	report_date date NOT NULL, -- Fecha del reporte (dd/mm/aaaa)
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT maritim_activity_report_flag_check CHECK (((flag)::text = ANY ((ARRAY['GREEN'::character varying, 'YELLOW'::character varying, 'RED'::character varying])::text[]))),
	CONSTRAINT maritim_activity_report_pkey PRIMARY KEY (id),
	CONSTRAINT fk_maritim_activity_report_created_by FOREIGN KEY (created_by) REFERENCES public._user(id),
	CONSTRAINT fk_maritim_activity_report_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES public._user(id)
);
CREATE INDEX idx_maritim_activity_report_activity ON public.maritim_activity_report USING btree (activity);
CREATE INDEX idx_maritim_activity_report_country_city ON public.maritim_activity_report USING btree (country, city);
CREATE INDEX idx_maritim_activity_report_date ON public.maritim_activity_report USING btree (report_date);
CREATE INDEX idx_maritim_activity_report_flag ON public.maritim_activity_report USING btree (flag);
COMMENT ON TABLE public.maritim_activity_report IS 'Reportes de actividades marítimas enviados por DIMAR';

-- Column comments

COMMENT ON COLUMN public.maritim_activity_report.country IS 'País donde se realiza la actividad';
COMMENT ON COLUMN public.maritim_activity_report.city IS 'Ciudad donde se realiza la actividad';
COMMENT ON COLUMN public.maritim_activity_report.activity IS 'Nombre de la actividad marítima (ej: TOUR BAHIA, KITE SURFING, etc.)';
COMMENT ON COLUMN public.maritim_activity_report.flag IS 'Bandera de alerta: GREEN, YELLOW, RED';
COMMENT ON COLUMN public.maritim_activity_report.report_date IS 'Fecha del reporte (dd/mm/aaaa)';


-- public.request_provider_gallery definition

-- Drop table

-- DROP TABLE public.request_provider_gallery;

CREATE TABLE public.request_provider_gallery (
	id serial4 NOT NULL,
	request_id int4 NOT NULL,
	image_url text NOT NULL,
	description text NULL,
	order_index int4 NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	document_type_id int4 NOT NULL,
	CONSTRAINT request_provider_gallery_pkey PRIMARY KEY (id),
	CONSTRAINT fk_document_type FOREIGN KEY (document_type_id) REFERENCES public.request_provider_document_type(id)
);


-- public.service definition

-- Drop table

-- DROP TABLE public.service;

CREATE TABLE public.service (
	id serial4 NOT NULL,
	"name" varchar NULL,
	service_type varchar NULL,
	description text NULL,
	cancellation_policy text NULL,
	status varchar NULL,
	created_date timestamp NOT NULL,
	last_modified_date timestamp NULL,
	create_by int4 NOT NULL,
	last_modified__by int4 NULL,
	service_type_id int4 NULL,
	CONSTRAINT service_pkey PRIMARY KEY (id),
	CONSTRAINT service_service_type_id_fkey FOREIGN KEY (service_type_id) REFERENCES public.service_type(id)
);


-- public.shopping_cart definition

-- Drop table

-- DROP TABLE public.shopping_cart;

CREATE TABLE public.shopping_cart (
	id serial4 NOT NULL,
	user_id int4 NOT NULL,
	status varchar(50) NOT NULL,
	created_date timestamp NOT NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT shopping_cart_pkey PRIMARY KEY (id),
	CONSTRAINT fk_shopping_cart_user FOREIGN KEY (user_id) REFERENCES public._user(id) ON DELETE CASCADE
);


-- public.state definition

-- Drop table

-- DROP TABLE public.state;

CREATE TABLE public.state (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	country_id int4 NOT NULL,
	CONSTRAINT state_pkey PRIMARY KEY (id),
	CONSTRAINT state_country_fk FOREIGN KEY (country_id) REFERENCES public.country(id)
);


-- public."token" definition

-- Drop table

-- DROP TABLE public."token";

CREATE TABLE public."token" (
	id serial4 NOT NULL,
	created_at timestamp NULL,
	expires_at timestamp NULL,
	"token" varchar(50) NULL,
	validated_at timestamp NULL,
	user_id int4 NOT NULL,
	CONSTRAINT token_pkey PRIMARY KEY (id),
	CONSTRAINT token_user_id_fkey FOREIGN KEY (user_id) REFERENCES public._user(id)
);


-- public.user_roles definition

-- Drop table

-- DROP TABLE public.user_roles;

CREATE TABLE public.user_roles (
	user_id int4 NOT NULL,
	role_id int4 NOT NULL,
	CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public."role"(id),
	CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public._user(id)
);


-- public.city definition

-- Drop table

-- DROP TABLE public.city;

CREATE TABLE public.city (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	state_id int4 NOT NULL,
	CONSTRAINT city_pkey PRIMARY KEY (id),
	CONSTRAINT city_state_fk FOREIGN KEY (state_id) REFERENCES public.state(id)
);


-- public.provider definition

-- Drop table

-- DROP TABLE public.provider;

CREATE TABLE public.provider (
	id serial4 NOT NULL,
	"name" varchar(100) NULL,
	service_type varchar(30) NOT NULL,
	document_number varchar(30) NOT NULL,
	document_type varchar(10) NOT NULL,
	country_id int4 NOT NULL,
	state_id int4 NOT NULL,
	city_id int4 NOT NULL,
	department varchar(100) NOT NULL,
	address varchar(50) NOT NULL,
	phone varchar(20) NOT NULL,
	status varchar NULL,
	user_id int4 NOT NULL,
	created_date timestamp NOT NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT provider_pkey PRIMARY KEY (id),
	CONSTRAINT provider_city_id_fk FOREIGN KEY (city_id) REFERENCES public.city(id),
	CONSTRAINT provider_country_id_fk FOREIGN KEY (country_id) REFERENCES public.country(id),
	CONSTRAINT provider_state_id_fk FOREIGN KEY (state_id) REFERENCES public.state(id),
	CONSTRAINT provider_user_id_fkey FOREIGN KEY (user_id) REFERENCES public._user(id)
);


-- public.request_provider definition

-- Drop table

-- DROP TABLE public.request_provider;

CREATE TABLE public.request_provider (
	id serial4 NOT NULL,
	status varchar NULL,
	provider_id int4 NOT NULL,
	created_date timestamp NOT NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	declined_reason text NULL,
	incomplete_reason text NULL,
	CONSTRAINT request_provider_pkey PRIMARY KEY (id),
	CONSTRAINT request_provider_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES public.provider(id)
);


-- public.tour definition

-- Drop table

-- DROP TABLE public.tour;

CREATE TABLE public.tour (
	id serial4 NOT NULL,
	"name" jsonb NOT NULL,
	description jsonb NULL,
	category_id int4 NOT NULL,
	duration text NULL,
	max_people int4 NULL,
	highlight int4 NULL,
	provider_id int4 NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	status varchar(30) NOT NULL,
	min_age int4 NULL,
	rating numeric NULL,
	duration_type public."duration_type_enum" NULL,
	service_id int4 NULL,
	CONSTRAINT tour_description_es_required CHECK (((description IS NULL) OR (((description ->> 'es'::text) IS NOT NULL) AND ((description ->> 'es'::text) <> ''::text)))),
	CONSTRAINT tour_pkey PRIMARY KEY (id),
	CONSTRAINT tour_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.tour_category(id),
	CONSTRAINT tour_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES public.provider(id),
	CONSTRAINT tour_service_id_fkey FOREIGN KEY (service_id) REFERENCES public.service(id),
	CONSTRAINT tour_service_id_fkey1 FOREIGN KEY (service_id) REFERENCES public.service(id)
);
CREATE INDEX idx_tour_description_es ON public.tour USING btree (((description ->> 'es'::text)));
CREATE INDEX idx_tour_description_gin ON public.tour USING gin (description);


-- public.tour_address definition

-- Drop table

-- DROP TABLE public.tour_address;

CREATE TABLE public.tour_address (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	country_id int4 NOT NULL,
	state_id int4 NOT NULL,
	city_id int4 NOT NULL,
	latitude numeric NULL,
	longitude numeric NULL,
	address text NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	"address_type" varchar(30) NULL,
	"location" jsonb NULL,
	CONSTRAINT address_pkey PRIMARY KEY (id),
	CONSTRAINT tour_address_location_es_required CHECK (((location IS NULL) OR (((location ->> 'es'::text) IS NOT NULL) AND ((location ->> 'es'::text) <> ''::text)))),
	CONSTRAINT address_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id),
	CONSTRAINT tour_address_city_fk FOREIGN KEY (city_id) REFERENCES public.city(id),
	CONSTRAINT tour_address_country_fk FOREIGN KEY (country_id) REFERENCES public.country(id),
	CONSTRAINT tour_address_state_fk FOREIGN KEY (state_id) REFERENCES public.state(id)
);
CREATE INDEX idx_tour_address_location_es ON public.tour_address USING btree (((location ->> 'es'::text)));
CREATE INDEX idx_tour_address_location_gin ON public.tour_address USING gin (location);


-- public.tour_cancellation_policy definition

-- Drop table

-- DROP TABLE public.tour_cancellation_policy;

CREATE TABLE public.tour_cancellation_policy (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	cancellation_policy_type varchar(30) NOT NULL,
	allows_rain_refund bool DEFAULT true NULL,
	allows_rescheduling bool DEFAULT true NULL,
	observations jsonb NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT chk_cancellation_policy_type CHECK (((cancellation_policy_type)::text = ANY ((ARRAY['Flexible'::character varying, 'Standard'::character varying, 'Moderate'::character varying, 'Strict'::character varying, 'Non-refundable'::character varying])::text[]))),
	CONSTRAINT tour_cancellation_policy_observations_es_required CHECK (((observations IS NULL) OR (((observations ->> 'es'::text) IS NOT NULL) AND ((observations ->> 'es'::text) <> ''::text)))),
	CONSTRAINT tour_cancellation_policy_pkey PRIMARY KEY (id),
	CONSTRAINT tour_cancellation_policy_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);
CREATE INDEX idx_tour_cancellation_policy_observations_gin ON public.tour_cancellation_policy USING gin (observations);


-- public.tour_faq definition

-- Drop table

-- DROP TABLE public.tour_faq;

CREATE TABLE public.tour_faq (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	question jsonb NULL,
	answer jsonb NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT faq_pkey PRIMARY KEY (id),
	CONSTRAINT tour_faq_answer_es_required CHECK (((answer IS NULL) OR (((answer ->> 'es'::text) IS NOT NULL) AND ((answer ->> 'es'::text) <> ''::text)))),
	CONSTRAINT tour_faq_question_es_required CHECK (((question IS NULL) OR (((question ->> 'es'::text) IS NOT NULL) AND ((question ->> 'es'::text) <> ''::text)))),
	CONSTRAINT faq_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);
CREATE INDEX idx_tour_faq_answer_gin ON public.tour_faq USING gin (answer);
CREATE INDEX idx_tour_faq_question_gin ON public.tour_faq USING gin (question);


-- public.tour_gallery definition

-- Drop table

-- DROP TABLE public.tour_gallery;

CREATE TABLE public.tour_gallery (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	image_url text NULL,
	description jsonb NULL,
	order_index int4 NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT gallery_pkey PRIMARY KEY (id),
	CONSTRAINT tour_gallery_description_es_required CHECK (((description IS NULL) OR (((description ->> 'es'::text) IS NOT NULL) AND ((description ->> 'es'::text) <> ''::text)))),
	CONSTRAINT gallery_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);
CREATE INDEX idx_tour_gallery_description_gin ON public.tour_gallery USING gin (description);


-- public.tour_includes_excludes definition

-- Drop table

-- DROP TABLE public.tour_includes_excludes;

CREATE TABLE public.tour_includes_excludes (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	description jsonb NULL,
	"type" varchar(30) NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT includes_excludes_pkey PRIMARY KEY (id),
	CONSTRAINT tour_includes_excludes_description_es_required CHECK (((description IS NULL) OR (((description ->> 'es'::text) IS NOT NULL) AND ((description ->> 'es'::text) <> ''::text)))),
	CONSTRAINT includes_excludes_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);
CREATE INDEX idx_tour_includes_excludes_description_es ON public.tour_includes_excludes USING btree (((description ->> 'es'::text)));
CREATE INDEX idx_tour_includes_excludes_description_gin ON public.tour_includes_excludes USING gin (description);


-- public.tour_itinerary definition

-- Drop table

-- DROP TABLE public.tour_itinerary;

CREATE TABLE public.tour_itinerary (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	title jsonb NULL,
	"day" int4 NULL,
	"time" time NULL,
	description jsonb NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT itinerary_pkey PRIMARY KEY (id),
	CONSTRAINT tour_itinerary_description_es_required CHECK (((description IS NULL) OR (((description ->> 'es'::text) IS NOT NULL) AND ((description ->> 'es'::text) <> ''::text)))),
	CONSTRAINT tour_itinerary_title_es_required CHECK (((title IS NULL) OR (((title ->> 'es'::text) IS NOT NULL) AND ((title ->> 'es'::text) <> ''::text)))),
	CONSTRAINT itinerary_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);
CREATE INDEX idx_tour_itinerary_description_gin ON public.tour_itinerary USING gin (description);
CREATE INDEX idx_tour_itinerary_title_gin ON public.tour_itinerary USING gin (title);


-- public.tour_main_attractions definition

-- Drop table

-- DROP TABLE public.tour_main_attractions;

CREATE TABLE public.tour_main_attractions (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	description jsonb NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	last_modified_by int4 NULL,
	created_by int4 NOT NULL,
	CONSTRAINT main_attractions_pkey PRIMARY KEY (id),
	CONSTRAINT tour_main_attractions_description_es_required CHECK (((description IS NULL) OR (((description ->> 'es'::text) IS NOT NULL) AND ((description ->> 'es'::text) <> ''::text)))),
	CONSTRAINT main_attractions_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);
CREATE INDEX idx_tour_main_attractions_description_es ON public.tour_main_attractions USING btree (((description ->> 'es'::text)));
CREATE INDEX idx_tour_main_attractions_description_gin ON public.tour_main_attractions USING gin (description);


-- public.tour_price definition

-- Drop table

-- DROP TABLE public.tour_price;

CREATE TABLE public.tour_price (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	"person_type" public."person_type" NULL,
	min_age int4 NULL,
	max_age int4 NULL,
	price numeric NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	CONSTRAINT price_pkey PRIMARY KEY (id),
	CONSTRAINT price_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);


-- public.tour_review definition

-- Drop table

-- DROP TABLE public.tour_review;

CREATE TABLE public.tour_review (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	author text NULL,
	rating int4 NULL,
	reason public."review_reason" NULL,
	"text" text NULL,
	reply_to int4 NULL,
	is_operator bool NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	CONSTRAINT review_pkey PRIMARY KEY (id),
	CONSTRAINT review_rating_check CHECK (((rating >= 1) AND (rating <= 5))),
	CONSTRAINT review_reply_to_fkey FOREIGN KEY (reply_to) REFERENCES public.tour_review(id),
	CONSTRAINT review_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);


-- public.tour_review_image definition

-- Drop table

-- DROP TABLE public.tour_review_image;

CREATE TABLE public.tour_review_image (
	id serial4 NOT NULL,
	review_id int4 NULL,
	image_url text NULL,
	description text NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	CONSTRAINT review_image_pkey PRIMARY KEY (id),
	CONSTRAINT review_image_review_id_fkey FOREIGN KEY (review_id) REFERENCES public.tour_review(id)
);


-- public.tour_schedule_config definition

-- Drop table

-- DROP TABLE public.tour_schedule_config;

CREATE TABLE public.tour_schedule_config (
	id serial4 NOT NULL,
	tour_id int4 NULL,
	"label" text NULL,
	start_date date NULL,
	end_date date NULL,
	days_of_week _text NULL,
	is_unlimited_capacity bool DEFAULT false NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	provider_id int4 NULL,
	is_template bool NULL,
	CONSTRAINT chk_tsc_tour_or_provider CHECK (((tour_id IS NOT NULL) OR (provider_id IS NOT NULL))) NOT VALID,
	CONSTRAINT tour_schedule_config_pkey PRIMARY KEY (id),
	CONSTRAINT tour_schedule_config_provider_id_fkey FOREIGN KEY (provider_id) REFERENCES public.provider(id),
	CONSTRAINT tour_schedule_config_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);
CREATE INDEX idx_tsc_provider_id ON public.tour_schedule_config USING btree (provider_id);
CREATE INDEX idx_tsc_tour_id ON public.tour_schedule_config USING btree (tour_id);


-- public.tour_schedule_config_slot definition

-- Drop table

-- DROP TABLE public.tour_schedule_config_slot;

CREATE TABLE public.tour_schedule_config_slot (
	id serial4 NOT NULL,
	config_id int4 NOT NULL,
	start_time time NOT NULL,
	end_time time NOT NULL,
	min_capacity int4 NULL,
	max_capacity int4 NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	CONSTRAINT tour_schedule_config_slot_pkey PRIMARY KEY (id),
	CONSTRAINT tour_schedule_config_slot_config_id_fkey FOREIGN KEY (config_id) REFERENCES public.tour_schedule_config(id)
);


-- public.tour_status_history definition

-- Drop table

-- DROP TABLE public.tour_status_history;

CREATE TABLE public.tour_status_history (
	id serial4 NOT NULL,
	tour_id int4 NOT NULL,
	status text NOT NULL,
	changed_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	changed_by int4 NULL,
	observations text NULL,
	CONSTRAINT tour_status_history_pkey PRIMARY KEY (id),
	CONSTRAINT tour_status_history_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);


-- public.tour_tag_mapping definition

-- Drop table

-- DROP TABLE public.tour_tag_mapping;

CREATE TABLE public.tour_tag_mapping (
	tour_id int4 NOT NULL,
	tag_id int4 NOT NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT tour_tag_mapping_pkey PRIMARY KEY (tour_id, tag_id),
	CONSTRAINT fk_tag FOREIGN KEY (tag_id) REFERENCES public.tour_tag(id) ON DELETE CASCADE,
	CONSTRAINT fk_tour FOREIGN KEY (tour_id) REFERENCES public.tour(id) ON DELETE CASCADE
);


-- public.tour_schedule definition

-- Drop table

-- DROP TABLE public.tour_schedule;

CREATE TABLE public.tour_schedule (
	id serial4 NOT NULL,
	tour_id int4 NOT NULL,
	schedule_date date NOT NULL,
	max_capacity int4 NULL,
	reserved_capacity int4 DEFAULT 0 NULL,
	is_unlimited_capacity bool DEFAULT false NULL,
	status varchar(20) DEFAULT 'available'::character varying NULL,
	config_id int4 NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	CONSTRAINT tour_schedule_pkey PRIMARY KEY (id),
	CONSTRAINT uq_tour_schedule_tour_date UNIQUE (tour_id, schedule_date),
	CONSTRAINT tour_schedule_config_id_fkey FOREIGN KEY (config_id) REFERENCES public.tour_schedule_config(id),
	CONSTRAINT tour_schedule_tour_id_fkey FOREIGN KEY (tour_id) REFERENCES public.tour(id)
);
CREATE INDEX idx_tour_schedule_tour_date ON public.tour_schedule USING btree (tour_id, schedule_date);


-- public.tour_schedule_config_price definition

-- Drop table

-- DROP TABLE public.tour_schedule_config_price;

CREATE TABLE public.tour_schedule_config_price (
	id serial4 NOT NULL,
	slot_id int4 NOT NULL,
	age_type public."age_price_type_enum" DEFAULT 'ADULT'::age_price_type_enum NOT NULL,
	min_age int4 NOT NULL,
	max_age int4 NOT NULL,
	price numeric NOT NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp NULL,
	CONSTRAINT tour_schedule_config_price_pkey PRIMARY KEY (id),
	CONSTRAINT tour_schedule_config_price_slot_id_fkey FOREIGN KEY (slot_id) REFERENCES public.tour_schedule_config_slot(id)
);


-- public.tour_schedule_status_history definition

-- Drop table

-- DROP TABLE public.tour_schedule_status_history;

CREATE TABLE public.tour_schedule_status_history (
	id serial4 NOT NULL,
	schedule_id int4 NOT NULL,
	status text NOT NULL,
	changed_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	changed_by int4 NULL,
	observations text NULL,
	CONSTRAINT tour_schedule_status_history_pkey PRIMARY KEY (id),
	CONSTRAINT tour_schedule_status_history_schedule_id_fkey FOREIGN KEY (schedule_id) REFERENCES public.tour_schedule(id)
);


-- public.tour_reservation definition

-- Drop table

-- DROP TABLE public.tour_reservation;

CREATE TABLE public.tour_reservation (
	id serial4 NOT NULL,
	schedule_id int4 NOT NULL,
	user_id int4 NOT NULL,
	client_name text NOT NULL,
	client_email text NOT NULL,
	client_phone text NULL,
	payment_method text NULL,
	status public."reservation_status" DEFAULT 'PENDING'::reservation_status NOT NULL,
	total_amount numeric(10, 2) NOT NULL,
	currency varchar(3) NOT NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	created_date timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamptz NULL,
	CONSTRAINT tour_reservation_pkey PRIMARY KEY (id),
	CONSTRAINT tour_reservation_schedule_id_fkey FOREIGN KEY (schedule_id) REFERENCES public.tour_schedule(id),
	CONSTRAINT tour_reservation_user_id_fkey FOREIGN KEY (user_id) REFERENCES public._user(id)
);
CREATE INDEX tour_reservation_schedule_id_idx ON public.tour_reservation USING btree (schedule_id);
CREATE INDEX tour_reservation_status_idx ON public.tour_reservation USING btree (status);
CREATE INDEX tour_reservation_user_id_idx ON public.tour_reservation USING btree (user_id);


-- public.tour_reservation_detail definition

-- Drop table

-- DROP TABLE public.tour_reservation_detail;

CREATE TABLE public.tour_reservation_detail (
	id serial4 NOT NULL,
	reservation_id int4 NOT NULL,
	price_id int4 NOT NULL,
	quantity int4 NOT NULL,
	price_at_reservation numeric(10, 2) NOT NULL,
	subtotal numeric(10, 2) NOT NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	created_date timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamptz NULL,
	CONSTRAINT tour_reservation_detail_pkey PRIMARY KEY (id),
	CONSTRAINT tour_reservation_detail_quantity_check CHECK ((quantity > 0)),
	CONSTRAINT tour_reservation_detail_price_id_fkey FOREIGN KEY (price_id) REFERENCES public.tour_schedule_config_price(id),
	CONSTRAINT tour_reservation_detail_reservation_id_fkey FOREIGN KEY (reservation_id) REFERENCES public.tour_reservation(id) ON DELETE CASCADE
);
CREATE INDEX tour_reservation_detail_price_id_idx ON public.tour_reservation_detail USING btree (price_id);
CREATE INDEX tour_reservation_detail_reservation_id_idx ON public.tour_reservation_detail USING btree (reservation_id);


-- public.tour_reservation_status_history definition

-- Drop table

-- DROP TABLE public.tour_reservation_status_history;

CREATE TABLE public.tour_reservation_status_history (
	id serial4 NOT NULL,
	reservation_id int4 NOT NULL,
	status text NOT NULL,
	changed_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	changed_by int4 NULL,
	observations text NULL,
	CONSTRAINT tour_reservation_status_history_pkey PRIMARY KEY (id),
	CONSTRAINT tour_reservation_status_history_reservation_id_fkey FOREIGN KEY (reservation_id) REFERENCES public.tour_reservation(id)
);


-- public.account_payable definition

-- Drop table

-- DROP TABLE public.account_payable;

CREATE TABLE public.account_payable (
	id bigserial NOT NULL,
	reservation_id int8 NOT NULL,
	provider_id int8 NOT NULL,
	transaction_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	amount numeric(10, 2) NOT NULL,
	delivery_status public."account_payable_status_enum" DEFAULT 'PENDING'::account_payable_status_enum NOT NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	last_modified_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by varchar(255) NULL,
	last_modified_by varchar(255) NULL,
	CONSTRAINT account_payable_pkey PRIMARY KEY (id)
);


-- public.credit definition

-- Drop table

-- DROP TABLE public.credit;

CREATE TABLE public.credit (
	id bigserial NOT NULL,
	reservation_id int8 NOT NULL, -- ID de la reserva que generó el crédito
	amount numeric(10, 2) NOT NULL, -- Monto del crédito
	creation_date date NOT NULL, -- Fecha en que se creó el crédito
	expiration_date date NOT NULL, -- Fecha de vencimiento del crédito (1 año desde la creación)
	status varchar(20) DEFAULT 'CREATED'::character varying NOT NULL, -- Estado del crédito: CREATED, CANCELED, DELETED
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT chk_credit_amount_positive CHECK ((amount >= (0)::numeric)),
	CONSTRAINT chk_credit_expiration_date CHECK ((expiration_date >= creation_date)),
	CONSTRAINT credit_pkey PRIMARY KEY (id),
	CONSTRAINT credit_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'CANCELED'::character varying, 'DELETED'::character varying])::text[])))
);
CREATE INDEX idx_credit_creation_date ON public.credit USING btree (creation_date);
CREATE INDEX idx_credit_expiration_date ON public.credit USING btree (expiration_date);
CREATE INDEX idx_credit_reservation_id ON public.credit USING btree (reservation_id);
CREATE INDEX idx_credit_status ON public.credit USING btree (status);
COMMENT ON TABLE public.credit IS 'Créditos generados por cancelación o re-agendamiento de reservas';

-- Column comments

COMMENT ON COLUMN public.credit.reservation_id IS 'ID de la reserva que generó el crédito';
COMMENT ON COLUMN public.credit.amount IS 'Monto del crédito';
COMMENT ON COLUMN public.credit.creation_date IS 'Fecha en que se creó el crédito';
COMMENT ON COLUMN public.credit.expiration_date IS 'Fecha de vencimiento del crédito (1 año desde la creación)';
COMMENT ON COLUMN public.credit.status IS 'Estado del crédito: CREATED, CANCELED, DELETED';


-- public.reservation definition

-- Drop table

-- DROP TABLE public.reservation;

CREATE TABLE public.reservation (
	reservation_id bigserial NOT NULL,
	payment_id int8 NOT NULL,
	qr_url varchar(500) NULL,
	reservation_date timestamp NOT NULL,
	delivery_status varchar(50) DEFAULT 'PENDING'::character varying NOT NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	item_id int8 DEFAULT 0 NOT NULL, -- ID del item del carrito de compras asociado a esta reserva
	service_responsible_name varchar(255) NULL, -- Nombre del responsable del servicio
	service_responsible_email varchar(255) NULL, -- Email del responsable del servicio
	service_responsible_phone varchar(20) NULL, -- Teléfono del responsable del servicio
	max_cancellation_date date NULL, -- Fecha máxima permitida para cancelar la reserva según la política del tour
	max_rescheduling_date date NULL, -- Fecha máxima permitida para re-agendar la reserva (2 días antes del tour)
	cancellation_reason varchar(20) NULL, -- Motivo de cancelación: CANNOT_ATTEND o RAIN
	cancellation_date timestamp NULL, -- Fecha y hora en que se canceló la reserva
	CONSTRAINT reservation_pkey PRIMARY KEY (reservation_id)
);
CREATE INDEX idx_reservation_delivery_status ON public.reservation USING btree (delivery_status);
CREATE INDEX idx_reservation_item_id ON public.reservation USING btree (item_id);
CREATE INDEX idx_reservation_payment_id ON public.reservation USING btree (payment_id);
CREATE INDEX idx_reservation_qr_url ON public.reservation USING btree (qr_url);
CREATE INDEX idx_reservation_reservation_date ON public.reservation USING btree (reservation_date);

-- Column comments

COMMENT ON COLUMN public.reservation.item_id IS 'ID del item del carrito de compras asociado a esta reserva';
COMMENT ON COLUMN public.reservation.service_responsible_name IS 'Nombre del responsable del servicio';
COMMENT ON COLUMN public.reservation.service_responsible_email IS 'Email del responsable del servicio';
COMMENT ON COLUMN public.reservation.service_responsible_phone IS 'Teléfono del responsable del servicio';
COMMENT ON COLUMN public.reservation.max_cancellation_date IS 'Fecha máxima permitida para cancelar la reserva según la política del tour';
COMMENT ON COLUMN public.reservation.max_rescheduling_date IS 'Fecha máxima permitida para re-agendar la reserva (2 días antes del tour)';
COMMENT ON COLUMN public.reservation.cancellation_reason IS 'Motivo de cancelación: CANNOT_ATTEND o RAIN';
COMMENT ON COLUMN public.reservation.cancellation_date IS 'Fecha y hora en que se canceló la reserva';


-- public.review definition

-- Drop table

-- DROP TABLE public.review;

CREATE TABLE public.review (
	id bigserial NOT NULL,
	reservation_id int8 NOT NULL, -- Reference to the reservation this review is for
	item_id int8 NOT NULL, -- Reference to the shopping cart item
	tour_id int4 NOT NULL, -- Reference to the tour being reviewed
	user_id int4 NOT NULL, -- Reference to the user who wrote the review
	rating numeric(3, 2) NOT NULL, -- Rating from 1 to 5
	"comment" jsonb NULL, -- Review comment in multiple languages (JSONB)
	status varchar(20) DEFAULT 'PENDING'::character varying NOT NULL, -- Status of the review: PENDING, PUBLISHED, CANCELED
	review_date date NOT NULL,
	likes int4 DEFAULT 0 NOT NULL,
	dislikes int4 DEFAULT 0 NOT NULL,
	hearts int4 DEFAULT 0 NOT NULL,
	rejection_reason text NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT pk_review_id PRIMARY KEY (id),
	CONSTRAINT review_rating_check1 CHECK (((rating >= (1)::numeric) AND (rating <= (5)::numeric)))
);
CREATE INDEX idx_review_comment_gin ON public.review USING gin (comment);
CREATE INDEX idx_review_item_id ON public.review USING btree (item_id);
CREATE INDEX idx_review_rating ON public.review USING btree (rating);
CREATE INDEX idx_review_reservation_id ON public.review USING btree (reservation_id);
CREATE INDEX idx_review_review_date ON public.review USING btree (review_date);
CREATE INDEX idx_review_status ON public.review USING btree (status);
CREATE INDEX idx_review_tour_id ON public.review USING btree (tour_id);
CREATE INDEX idx_review_user_id ON public.review USING btree (user_id);
COMMENT ON TABLE public.review IS 'Table storing reviews/ratings for tours';

-- Column comments

COMMENT ON COLUMN public.review.reservation_id IS 'Reference to the reservation this review is for';
COMMENT ON COLUMN public.review.item_id IS 'Reference to the shopping cart item';
COMMENT ON COLUMN public.review.tour_id IS 'Reference to the tour being reviewed';
COMMENT ON COLUMN public.review.user_id IS 'Reference to the user who wrote the review';
COMMENT ON COLUMN public.review.rating IS 'Rating from 1 to 5';
COMMENT ON COLUMN public.review."comment" IS 'Review comment in multiple languages (JSONB)';
COMMENT ON COLUMN public.review.status IS 'Status of the review: PENDING, PUBLISHED, CANCELED';


-- public.review_answer definition

-- Drop table

-- DROP TABLE public.review_answer;

CREATE TABLE public.review_answer (
	answer_id bigserial NOT NULL,
	review_id int8 NOT NULL,
	"comment" jsonb NULL, -- Answer comment in multiple languages (JSONB)
	provider_name varchar(255) NULL,
	provider_image varchar(500) NULL,
	"date" date NULL,
	likes int4 DEFAULT 0 NOT NULL,
	dislikes int4 DEFAULT 0 NOT NULL,
	hearts int4 DEFAULT 0 NOT NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT pk_review_answer_id PRIMARY KEY (answer_id),
	CONSTRAINT uq_review_answer_review_id UNIQUE (review_id)
);
CREATE INDEX idx_review_answer_comment_gin ON public.review_answer USING gin (comment);
CREATE INDEX idx_review_answer_review_id ON public.review_answer USING btree (review_id);
COMMENT ON TABLE public.review_answer IS 'Table storing provider answers/responses to reviews';

-- Column comments

COMMENT ON COLUMN public.review_answer."comment" IS 'Answer comment in multiple languages (JSONB)';


-- public.review_answer_attachment definition

-- Drop table

-- DROP TABLE public.review_answer_attachment;

CREATE TABLE public.review_answer_attachment (
	id bigserial NOT NULL,
	answer_id int8 NOT NULL,
	file_url varchar(500) NOT NULL,
	file_name varchar(255) NULL,
	file_type varchar(50) NULL,
	file_size int8 NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT pk_review_answer_attachment_id PRIMARY KEY (id)
);
CREATE INDEX idx_review_answer_attachment_answer_id ON public.review_answer_attachment USING btree (answer_id);
COMMENT ON TABLE public.review_answer_attachment IS 'Table storing file attachments (photos) for review answers';


-- public.review_attachment definition

-- Drop table

-- DROP TABLE public.review_attachment;

CREATE TABLE public.review_attachment (
	id bigserial NOT NULL,
	review_id int8 NOT NULL,
	file_url varchar(500) NOT NULL,
	file_name varchar(255) NULL,
	file_type varchar(50) NULL,
	file_size int8 NULL,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	last_modified_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	CONSTRAINT pk_review_attachment_id PRIMARY KEY (id)
);
CREATE INDEX idx_review_attachment_review_id ON public.review_attachment USING btree (review_id);
COMMENT ON TABLE public.review_attachment IS 'Table storing file attachments (photos) for reviews';


-- public.shopping_cart_item definition

-- Drop table

-- DROP TABLE public.shopping_cart_item;

CREATE TABLE public.shopping_cart_item (
	id serial4 NOT NULL,
	shopping_cart_id int4 NOT NULL,
	tour_schedule_id int4 NOT NULL,
	quantity int4 NULL,
	unit_price numeric(10, 2) NULL,
	total_price numeric(10, 2) NOT NULL,
	created_date timestamp NOT NULL,
	last_modified_date timestamp NULL,
	created_by int4 NOT NULL,
	last_modified_by int4 NULL,
	product_id int4 NULL,
	service_type_id int4 NULL,
	status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
	product_type varchar(50) DEFAULT 'TOUR'::character varying NOT NULL,
	schedule_date date NULL,
	slot_id int4 NULL,
	service_id int4 NULL,
	reservation_id int8 NULL,
	CONSTRAINT shopping_cart_item_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_shopping_cart_item_reservation_id ON public.shopping_cart_item USING btree (reservation_id);


-- public.account_payable foreign keys

ALTER TABLE public.account_payable ADD CONSTRAINT fk_account_payable_provider FOREIGN KEY (provider_id) REFERENCES public.provider(id) ON DELETE CASCADE;
ALTER TABLE public.account_payable ADD CONSTRAINT fk_account_payable_reservation FOREIGN KEY (reservation_id) REFERENCES public.reservation(reservation_id) ON DELETE CASCADE;


-- public.credit foreign keys

ALTER TABLE public.credit ADD CONSTRAINT fk_credit_created_by FOREIGN KEY (created_by) REFERENCES public._user(id);
ALTER TABLE public.credit ADD CONSTRAINT fk_credit_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES public._user(id);
ALTER TABLE public.credit ADD CONSTRAINT fk_credit_reservation FOREIGN KEY (reservation_id) REFERENCES public.reservation(reservation_id) ON DELETE CASCADE;


-- public.reservation foreign keys

ALTER TABLE public.reservation ADD CONSTRAINT fk_reservation_payment FOREIGN KEY (payment_id) REFERENCES public.payment(payment_id) ON DELETE CASCADE;
ALTER TABLE public.reservation ADD CONSTRAINT fk_reservation_shopping_cart_item FOREIGN KEY (item_id) REFERENCES public.shopping_cart_item(id);


-- public.review foreign keys

ALTER TABLE public.review ADD CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES public.reservation(reservation_id) ON DELETE CASCADE;
ALTER TABLE public.review ADD CONSTRAINT fk_review_shopping_cart_item FOREIGN KEY (item_id) REFERENCES public.shopping_cart_item(id) ON DELETE CASCADE;
ALTER TABLE public.review ADD CONSTRAINT fk_review_tour FOREIGN KEY (tour_id) REFERENCES public.tour(id) ON DELETE CASCADE;
ALTER TABLE public.review ADD CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES public._user(id) ON DELETE CASCADE;


-- public.review_answer foreign keys

ALTER TABLE public.review_answer ADD CONSTRAINT fk_review_answer_review FOREIGN KEY (review_id) REFERENCES public.review(id) ON DELETE CASCADE;


-- public.review_answer_attachment foreign keys

ALTER TABLE public.review_answer_attachment ADD CONSTRAINT fk_review_answer_attachment_answer FOREIGN KEY (answer_id) REFERENCES public.review_answer(answer_id) ON DELETE CASCADE;


-- public.review_attachment foreign keys

ALTER TABLE public.review_attachment ADD CONSTRAINT fk_review_attachment_review FOREIGN KEY (review_id) REFERENCES public.review(id) ON DELETE CASCADE;


-- public.shopping_cart_item foreign keys

ALTER TABLE public.shopping_cart_item ADD CONSTRAINT fk_shopping_cart_item_cart FOREIGN KEY (shopping_cart_id) REFERENCES public.shopping_cart(id) ON DELETE CASCADE;
ALTER TABLE public.shopping_cart_item ADD CONSTRAINT fk_shopping_cart_item_reservation FOREIGN KEY (reservation_id) REFERENCES public.reservation(reservation_id) ON DELETE SET NULL;
ALTER TABLE public.shopping_cart_item ADD CONSTRAINT fk_shopping_cart_item_service FOREIGN KEY (service_id) REFERENCES public.service(id);
-- Foreign key constraint for slot_id removed - not needed for business logic
-- Prices are stored in shopping_cart_item_detail and rescheduling uses new schedule prices
-- slot_id remains as a simple integer column for display purposes only
ALTER TABLE public.shopping_cart_item ADD CONSTRAINT fk_shopping_cart_item_tour_schedule FOREIGN KEY (tour_schedule_id) REFERENCES public.tour_schedule(id);
ALTER TABLE public.shopping_cart_item ADD CONSTRAINT shopping_cart_item_service_type_id_fkey FOREIGN KEY (service_type_id) REFERENCES public.service_type(id);
ALTER TABLE public.shopping_cart_item ADD CONSTRAINT shopping_cart_item_tour_schedule_id_fkey FOREIGN KEY (tour_schedule_id) REFERENCES public.tour_schedule(id);



-- DROP FUNCTION public.get_templates_by_provider(int4);

CREATE OR REPLACE FUNCTION public.get_templates_by_provider(p_provider_id integer)
 RETURNS SETOF jsonb
 LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    SELECT
        jsonb_build_object(
            'id', c.id,  -- antes config_id
            'tourId', c.tour_id,
            'label', c.label,
            'startDate', c.start_date,
            'endDate', c.end_date,
            'daysOfWeek', c.days_of_week,
            'isUnlimitedCapacity', c.is_unlimited_capacity,
            'createdBy', c.created_by,
            'lastModifiedBy', c.last_modified_by,
            'createdDate', c.created_date,
            'lastModifiedDate', c.last_modified_date,
            'providerId', c.provider_id,
            'isTemplate', c.is_template,
            'slots', (
                SELECT jsonb_agg(
                    jsonb_build_object(
                        'id', s.id, -- antes slot_id
                        'startTime', s.start_time,
                        'endTime', s.end_time,
                        'minCapacity', s.min_capacity,
                        'maxCapacity', s.max_capacity,
                        'prices', (
                            SELECT jsonb_agg(
                                jsonb_build_object(
                                    'id', p.id, -- 🔑 corregido
                                    'ageType', p.age_type,
                                    'minAge', p.min_age,
                                    'maxAge', p.max_age,
                                    'price', p.price
                                )
                            )
                            FROM public.tour_schedule_config_price p
                            WHERE p.slot_id = s.id
                        )
                    )
                )
                FROM public.tour_schedule_config_slot s
                WHERE s.config_id = c.id
            )
        )
    FROM public.tour_schedule_config c
    WHERE c.provider_id = p_provider_id
      AND c.is_template = true;
END;
$function$
;

-- DROP FUNCTION public.sp_clear_shopping_cart(int8);

CREATE OR REPLACE FUNCTION public.sp_clear_shopping_cart(p_cart_id bigint)
 RETURNS integer
 LANGUAGE plpgsql
AS $function$
DECLARE
    v_deleted_items INT := 0;
BEGIN
    -- Validar que el carrito exista
    IF NOT EXISTS (SELECT 1 FROM public.shopping_cart WHERE id = p_cart_id) THEN
        RAISE EXCEPTION 'El carrito con id % no existe', p_cart_id;
    END IF;

    -- Eliminar primero los detalles de los ítems del carrito
    DELETE FROM public.shopping_cart_item_detail
    WHERE shopping_cart_item_id IN (
        SELECT id FROM public.shopping_cart_item WHERE shopping_cart_id = p_cart_id
    );

    -- Eliminar los ítems del carrito y contar cuántos se eliminaron
    DELETE FROM public.shopping_cart_item
    WHERE shopping_cart_id = p_cart_id;

    -- Obtener cantidad de filas afectadas
  	GET DIAGNOSTICS v_deleted_items = ROW_COUNT;
    --GET DIAGNOSTICS v_deleted_items = ROW_COUNT;

    -- Finalmente, eliminar el carrito
    DELETE FROM public.shopping_cart WHERE id = p_cart_id;
    -- Retornar cantidad total de ítems eliminados
    RETURN v_deleted_items;
END;
$function$
;

-- DROP FUNCTION public.sp_get_all_tour_tags();

CREATE OR REPLACE FUNCTION public.sp_get_all_tour_tags()
 RETURNS TABLE(tag_id integer, category text, name text, description text)
 LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    SELECT
        id AS tag_id,
        category::TEXT,
        name,
        description
    FROM public.tour_tag
    ORDER BY category, name;
END;
$function$
;

-- DROP FUNCTION public.sp_get_categories_with_tours();

CREATE OR REPLACE FUNCTION public.sp_get_categories_with_tours()
 RETURNS TABLE(category_id integer, category_name text)
 LANGUAGE sql
AS $function$
SELECT DISTINCT
  c.id AS category_id,
  c.name AS category_name
FROM tour t
JOIN tour_category c ON t.category_id = c.id
WHERE t.status = 'accepted';
$function$
;

-- DROP FUNCTION public.sp_get_locations_with_tours();

CREATE OR REPLACE FUNCTION public.sp_get_locations_with_tours()
 RETURNS TABLE(country_id integer, country_name text, state_id integer, state_name text, city_id integer, city_name text)
 LANGUAGE sql
AS $function$
SELECT DISTINCT
  co.id AS country_id,
  co.name AS country_name,
  s.id AS state_id,
  s.name AS state_name,
  c.id AS city_id,
  c.name AS city_name
FROM tour t
JOIN tour_address a ON a.tour_id = t.id
JOIN country co ON co.id = a.country_id
JOIN state s ON s.id = a.state_id
JOIN city c ON c.id = a.city_id
WHERE t.status = 'accepted';
$function$
;

-- DROP FUNCTION public.sp_get_provider_reservations(int4, int8, varchar);

CREATE OR REPLACE FUNCTION public.sp_get_provider_reservations(p_provider_id integer DEFAULT NULL::integer, p_reservation_id bigint DEFAULT NULL::bigint, p_delivery_status character varying DEFAULT NULL::character varying)
 RETURNS TABLE(reservationid bigint, reservationdate timestamp without time zone, reservationdeliverystatus character varying, reservationcreateddate timestamp without time zone, paymentid bigint, paymenttransactionid character varying, payername character varying, payeremail character varying, payerphone character varying, payerdocumenttype character varying, payerdocumentnumber character varying, shoppingitemid integer, shoppingtotalprice numeric, shoppingunitprice numeric, shoppingquantity integer, producttype character varying, productid integer, totaltourists bigint, tourid integer, tourname jsonb, tourcategoryid integer, tourproviderid integer, tourscheduleid integer, scheduledate date, slotid integer, slottime_start time without time zone, slottime_end time without time zone, service_responsible_name character varying, service_responsible_email character varying, service_responsible_phone character varying, max_cancellation_date date, max_rescheduling_date date, cancellation_reason character varying, cancellation_date timestamp without time zone)
 LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    SELECT
        r.reservation_id,
        r.reservation_date,
        r.delivery_status,
        r.created_date,

        p.payment_id,
        p.transaction_id,
        p.payer_name,
        p.payer_email,
        p.payer_phone,
        p.payer_document_type,
        p.payer_document_number,

        sci.id AS shoppingItemId,
        sci.total_price,
        sci.unit_price,
        sci.quantity,

        sci.product_type,
        sci.product_id,

        -- TOTAL DE TURISTAS (SUM devuelve BIGINT ✔)
        COALESCE((
            SELECT SUM(scid.quantity)
            FROM shopping_cart_item_detail scid
            WHERE scid.shopping_cart_item_id = sci.id
        ), 0) AS totalTourists,

        t.id AS tourId,
        t.name AS tourName,
        t.category_id,
        t.provider_id,

        ts.id AS tourScheduleId,
        sci.schedule_date,

        tscs.id AS slotId,
        tscs.start_time,
        tscs.end_time,
        tscs.min_capacity,
        tscs.max_capacity,

        r.service_responsible_name,
        r.service_responsible_email,
        r.service_responsible_phone,

        -- Campos de cancelación y re-agendamiento
        r.max_cancellation_date,
        r.max_rescheduling_date,
        r.cancellation_reason,
        r.cancellation_date

    FROM reservation r
    JOIN shopping_cart_item sci ON sci.id = r.item_id
    LEFT JOIN payment p ON p.payment_id = r.payment_id
    LEFT JOIN tour_schedule ts ON ts.id = sci.tour_schedule_id
    LEFT JOIN tour_schedule_config_slot tscs ON tscs.id = sci.slot_id
    LEFT JOIN tour t ON t.id = sci.product_id

    WHERE
        (p_provider_id IS NULL OR t.provider_id = p_provider_id)
        AND (p_reservation_id IS NULL OR r.reservation_id = p_reservation_id)
        AND (p_delivery_status IS NULL OR r.delivery_status = p_delivery_status)

    GROUP BY
        r.reservation_id,
        p.payment_id,
        sci.id,
        t.id,
        ts.id,
        tscs.id;

END;
$function$
;

-- DROP FUNCTION public.sp_get_tag_categories();

CREATE OR REPLACE FUNCTION public.sp_get_tag_categories()
 RETURNS TABLE(category text)
 LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    SELECT unnest(enum_range(NULL::tour_tag_category_enum))::TEXT;
END;
$function$
;

-- DROP FUNCTION public.sp_get_tour_schedule(int4, date, date, varchar, int4, int4);

CREATE OR REPLACE FUNCTION public.sp_get_tour_schedule(p_tour_id integer DEFAULT NULL::integer, p_start_date date DEFAULT NULL::date, p_end_date date DEFAULT NULL::date, p_status character varying DEFAULT NULL::character varying, p_limit integer DEFAULT 10, p_offset integer DEFAULT 0)
 RETURNS TABLE(schedule_id integer, tour_id integer, schedule_date date, start_time time without time zone, end_time time without time zone, is_unlimited_capacity boolean, max_capacity integer, reserved_capacity integer, status character varying)
 LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    SELECT 
        ts.id AS schedule_id,
        ts.tour_id,
        ts.schedule_date,
        ts.start_time,
        ts.end_time,
        ts.is_unlimited_capacity,
        ts.max_capacity,
        ts.reserved_capacity,
        ts.status
    FROM public.tour_schedule ts
    WHERE 
        (p_tour_id IS NULL OR ts.tour_id = p_tour_id) AND
        (p_start_date IS NULL OR ts.schedule_date >= p_start_date) AND
        (p_end_date IS NULL OR ts.schedule_date <= p_end_date) AND
        (p_status IS NULL OR ts.status = p_status)
    ORDER BY ts.schedule_date, ts.start_time
    LIMIT p_limit OFFSET p_offset;
END;
$function$
;

-- DROP FUNCTION public.sp_get_tour_schedule_json(jsonb);

CREATE OR REPLACE FUNCTION public.sp_get_tour_schedule_json(filters jsonb)
 RETURNS TABLE(result jsonb)
 LANGUAGE plpgsql
AS $function$
DECLARE
  -- Paginación
  v_page int := COALESCE((filters->>'page')::int, 0);
  v_size int := COALESCE((filters->>'size')::int, 10);

  -- Fechas
  v_start_date date := COALESCE((filters->>'start_date')::date, (filters->>'startDate')::date);
  v_end_date   date := COALESCE((filters->>'end_date')::date,   (filters->>'endDate')::date);

  -- Filtros por ubicación (address del tour)
  v_state_txt text := NULLIF(COALESCE(filters->>'state', filters->>'stateId'), '');
  v_city_txt  text := NULLIF(COALESCE(filters->>'city',  filters->>'cityId'),  '');

  -- Filtros por ubicación del provider (opcionales)
  v_provider_state_txt text := NULLIF(filters->>'providerStateId', '');
  v_provider_city_txt  text := NULLIF(filters->>'providerCityId',  '');

  -- Otros filtros
  v_duration_txt      text := NULLIF(filters->>'duration','');
  v_category_txt      text := NULLIF(COALESCE(filters->>'category', filters->>'categoryId'), '');
  v_duration_type_txt text := NULLIF(COALESCE(filters->>'duration_type', filters->>'durationType'), '');
  v_age_type_txt      text := NULLIF(COALESCE(filters->>'age_type',      filters->>'ageType'),      '');

  -- Precios numéricos
  v_min_price numeric := COALESCE(NULLIF(filters->>'min_price','')::numeric,
                                   NULLIF(filters->>'minPrice','')::numeric);
  v_max_price numeric := COALESCE(NULLIF(filters->>'max_price','')::numeric,
                                   NULLIF(filters->>'maxPrice','')::numeric);

  -- Otros
  v_tag text := NULLIF(COALESCE(filters->>'tag', filters->>'tags'), '');
  v_text_search text := NULLIF(filters->>'textSearch','');

  -- FILTRO DE TOUR_ID
  v_tour_id int := (filters->>'tourId')::int;

  -- PARÁMETRO DE IDIOMA (opcional, por defecto 'es')
  v_language text := COALESCE(NULLIF(filters->>'language', ''), 'es');

BEGIN
  RETURN QUERY
  SELECT jsonb_build_object(
    'tour', jsonb_build_object(
      'id', t.id,
      'name', t.name,
      'description', t.description,
      'duration', t.duration,
      'durationType', t.duration_type,
      'rating', t.rating,
      'status', t.status,
      'tags', COALESCE((
        SELECT jsonb_agg(
          jsonb_build_object(
            'id', tg.id,
            'name', tg.name,
            'category', tg.category
          )
        )
        FROM tour_tag_mapping tm
        JOIN tour_tag tg ON tg.id = tm.tag_id
        WHERE tm.tour_id = t.id
      ), '[]'::jsonb),
      'address', jsonb_build_object(
        'country', a.country_id,
        'state', a.state_id,
        'city', a.city_id,
        'address', a.address,
        'latitude', a.latitude,
        'longitude', a.longitude
      ),
      'gallery', COALESCE((
        SELECT jsonb_agg(
          jsonb_build_object(
            'id', g.id,
            'imageUrl', g.image_url,
            'description', g.description,
            'order', g.order_index
          )
        )
        FROM tour_gallery g
        WHERE g.tour_id = t.id
      ), '[]'::jsonb)
    ),
    'schedules', CASE
      WHEN v_tour_id IS NOT NULL THEN
        COALESCE((
          SELECT jsonb_agg(
            jsonb_build_object(
              'id', s.id,
              'scheduleDate', s.schedule_date,
              'maxCapacity', s.max_capacity,
              'reservedCapacity', s.reserved_capacity,
              'isUnlimitedCapacity', s.is_unlimited_capacity,
              'status', s.status,
              'config', jsonb_build_object(
                'id', sc.id,
                'slots', COALESCE((
                  SELECT jsonb_agg(
                    jsonb_build_object(
                      'slotId', sl.id,
                      'startTime', sl.start_time,
                      'endTime', sl.end_time,
                      'minCapacity', sl.min_capacity,
                      'maxCapacity', sl.max_capacity,
                      'prices', COALESCE((
                        SELECT jsonb_agg(
                          jsonb_build_object(
                            'ageType', p.age_type,
                            'minAge', p.min_age,
                            'maxAge', p.max_age,
                            'price', p.price
                          )
                        )
                        FROM tour_schedule_config_price p
                        WHERE p.slot_id = sl.id
                        AND (v_age_type_txt IS NULL OR p.age_type::text = v_age_type_txt)
                        AND (v_min_price IS NULL OR p.price >= v_min_price)
                        AND (v_max_price IS NULL OR p.price <= v_max_price)
                      ), '[]'::jsonb),
                      'highestPrice', COALESCE((
                        SELECT jsonb_build_object(
                          'ageType', p.age_type,
                          'price', p.price
                        )
                        FROM tour_schedule_config_price p
                        WHERE p.slot_id = sl.id
                        AND (v_age_type_txt IS NULL OR p.age_type::text = v_age_type_txt)
                        AND (v_min_price IS NULL OR p.price >= v_min_price)
                        AND (v_max_price IS NULL OR p.price <= v_max_price)
                        ORDER BY p.price DESC
                        LIMIT 1
                      ), '{}'::jsonb)
                    )
                    ORDER BY sl.start_time
                  )
                  FROM tour_schedule_config_slot sl
                  WHERE sl.config_id = sc.id
                  AND (v_age_type_txt IS NULL OR EXISTS (SELECT 1 FROM tour_schedule_config_price px WHERE px.slot_id = sl.id AND px.age_type::text = v_age_type_txt))
                  AND (v_min_price IS NULL OR EXISTS (SELECT 1 FROM tour_schedule_config_price px WHERE px.slot_id = sl.id AND px.price >= v_min_price))
                  AND (v_max_price IS NULL OR EXISTS (SELECT 1 FROM tour_schedule_config_price px WHERE px.slot_id = sl.id AND px.price <= v_max_price))
                ), '[]'::jsonb)
              )
            )
            ORDER BY s.schedule_date ASC
          )
          FROM tour_schedule s
          LEFT JOIN tour_schedule_config sc ON sc.id = s.config_id
          WHERE s.tour_id = t.id
            AND (v_start_date IS NULL OR s.schedule_date >= v_start_date)
            AND (v_end_date IS NULL OR s.schedule_date <= v_end_date)
        ), '[]'::jsonb)
      ELSE
        (
          SELECT jsonb_agg(
            jsonb_build_object(
              'config', jsonb_build_object(
                'slots', jsonb_build_array(
                  jsonb_build_object(
                    'highestPrice', COALESCE((
                      SELECT jsonb_build_object(
                        'ageType', p.age_type,
                        'price', p.price
                      )
                      FROM tour_schedule s
                      JOIN tour_schedule_config sc ON sc.id = s.config_id
                      JOIN tour_schedule_config_slot sl ON sl.config_id = sc.id
                      JOIN tour_schedule_config_price p ON p.slot_id = sl.id
                      WHERE s.tour_id = t.id
                      AND (v_start_date IS NULL OR s.schedule_date >= v_start_date)
                      AND (v_end_date IS NULL OR s.schedule_date <= v_end_date)
                      ORDER BY p.price DESC
                      LIMIT 1
                    ), '{}'::jsonb)
                  )
                )
              )
            )
          )
        )
    END
  ) AS result
  FROM tour t
  JOIN tour_address a ON a.tour_id = t.id
  JOIN provider pr ON pr.id = t.provider_id
  WHERE
    t.status = 'accepted'
    AND (v_tour_id IS NULL OR t.id = v_tour_id)
    AND (v_state_txt IS NULL OR a.state_id::text = v_state_txt)
    AND (v_city_txt IS NULL OR a.city_id::text = v_city_txt)
    AND (v_provider_state_txt IS NULL OR pr.state_id::text = v_provider_state_txt)
    AND (v_provider_city_txt IS NULL OR pr.city_id::text = v_provider_city_txt)
    AND (v_duration_txt IS NULL OR t.duration::text = v_duration_txt)
    AND (v_duration_type_txt IS NULL OR t.duration_type::text = v_duration_type_txt)
    AND (v_category_txt IS NULL OR t.category_id::text = v_category_txt)
    AND (
      v_tag IS NULL OR EXISTS (
        SELECT 1
        FROM tour_tag_mapping tm2
        JOIN tour_tag tg2 ON tg2.id = tm2.tag_id
        WHERE tm2.tour_id = t.id
          AND tg2.name ILIKE '%' || v_tag || '%'
      )
    )
    AND (
      v_text_search IS NULL OR
      (t.name->>v_language) ILIKE '%' || v_text_search || '%' OR
      (t.description->>v_language) ILIKE '%' || v_text_search || '%'
    )
    AND (
      v_min_price IS NULL
      OR EXISTS (
        SELECT 1
        FROM tour_schedule s2
        JOIN tour_schedule_config sc2 ON sc2.id = s2.config_id
        JOIN tour_schedule_config_slot sl2 ON sl2.config_id = sc2.id
        JOIN tour_schedule_config_price p2 ON p2.slot_id = sl2.id
        WHERE s2.tour_id = t.id
          AND (v_start_date IS NULL OR s2.schedule_date >= v_start_date)
          AND (v_end_date IS NULL OR s2.schedule_date <= v_end_date)
          AND p2.price >= v_min_price
      )
    )
    AND (
      v_max_price IS NULL
      OR EXISTS (
        SELECT 1
        FROM tour_schedule s3
        JOIN tour_schedule_config sc3 ON sc3.id = s3.config_id
        JOIN tour_schedule_config_slot sl3 ON sl3.config_id = sc3.id
        JOIN tour_schedule_config_price p3 ON p3.slot_id = sl3.id
        WHERE s3.tour_id = t.id
          AND (v_start_date IS NULL OR s3.schedule_date >= v_start_date)
          AND (v_end_date IS NULL OR s3.schedule_date <= v_end_date)
          AND p3.price <= v_max_price
      )
    )
  GROUP BY t.id, a.id, pr.id
  ORDER BY t.id ASC
  LIMIT v_size
  OFFSET v_page * v_size;
END;
$function$
;

-- DROP FUNCTION public.sp_get_tour_tags(int4);

CREATE OR REPLACE FUNCTION public.sp_get_tour_tags(p_tour_id integer)
 RETURNS jsonb
 LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN (
        SELECT jsonb_agg(
            jsonb_build_object(
                'tagId', tt.id,
                'category', tt.category,
                'name', tt.name
            )
        )
        FROM public.tour_tag_mapping ttm
        JOIN public.tour_tag tt ON tt.id = ttm.tag_id
        WHERE ttm.tour_id = p_tour_id
        ORDER BY tt.category, tt.name
    );
END;
$function$
;

-- DROP FUNCTION public.uuid_generate_v1();

CREATE OR REPLACE FUNCTION public.uuid_generate_v1()
 RETURNS uuid
 LANGUAGE c
 PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v1$function$
;

-- DROP FUNCTION public.uuid_generate_v1mc();

CREATE OR REPLACE FUNCTION public.uuid_generate_v1mc()
 RETURNS uuid
 LANGUAGE c
 PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v1mc$function$
;

-- DROP FUNCTION public.uuid_generate_v3(uuid, text);

CREATE OR REPLACE FUNCTION public.uuid_generate_v3(namespace uuid, name text)
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v3$function$
;

-- DROP FUNCTION public.uuid_generate_v4();

CREATE OR REPLACE FUNCTION public.uuid_generate_v4()
 RETURNS uuid
 LANGUAGE c
 PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v4$function$
;

-- DROP FUNCTION public.uuid_generate_v5(uuid, text);

CREATE OR REPLACE FUNCTION public.uuid_generate_v5(namespace uuid, name text)
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_generate_v5$function$
;

-- DROP FUNCTION public.uuid_nil();

CREATE OR REPLACE FUNCTION public.uuid_nil()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_nil$function$
;

-- DROP FUNCTION public.uuid_ns_dns();

CREATE OR REPLACE FUNCTION public.uuid_ns_dns()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_ns_dns$function$
;

-- DROP FUNCTION public.uuid_ns_oid();

CREATE OR REPLACE FUNCTION public.uuid_ns_oid()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_ns_oid$function$
;

-- DROP FUNCTION public.uuid_ns_url();

CREATE OR REPLACE FUNCTION public.uuid_ns_url()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_ns_url$function$
;

-- DROP FUNCTION public.uuid_ns_x500();

CREATE OR REPLACE FUNCTION public.uuid_ns_x500()
 RETURNS uuid
 LANGUAGE c
 IMMUTABLE PARALLEL SAFE STRICT
AS '$libdir/uuid-ossp', $function$uuid_ns_x500$function$
;