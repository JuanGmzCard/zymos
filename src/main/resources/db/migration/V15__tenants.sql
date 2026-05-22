CREATE TABLE tenants (
    subdomain         VARCHAR(100) PRIMARY KEY,
    name              VARCHAR(200) NOT NULL,
    tagline           VARCHAR(300),
    logo_url          VARCHAR(500),
    color_navbar      VARCHAR(20)  NOT NULL DEFAULT '#242E0D',
    color_primary     VARCHAR(20)  NOT NULL DEFAULT '#364318',
    color_accent      VARCHAR(20)  NOT NULL DEFAULT '#C9A028',
    color_accent_hover VARCHAR(20) NOT NULL DEFAULT '#E0B840',
    color_cream       VARCHAR(20)  NOT NULL DEFAULT '#F5EDD0',
    color_body_bg     VARCHAR(20)  NOT NULL DEFAULT '#F0EDE2',
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP             DEFAULT NOW()
);

-- El tenant 'default' se crea en DataInitializer al arrancar.
