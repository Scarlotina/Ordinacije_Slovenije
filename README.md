Spodnja funkcija pridobi podatke od ordinacije
CREATE OR REPLACE FUNCTION get_ordinacija_data(search_text TEXT)
    RETURNS TABLE (
                      ime VARCHAR(200),
                      kontakt_osredotocen VARCHAR(200),
                      delovni_cas VARCHAR(200),
                      lokacija_ime VARCHAR(200),
                      specializacija_ime VARCHAR(200)
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT o.ime, o.kontakt_osredotocen, o.delovni_cas, l.ime AS Lokacija, s.ime AS Specializacija
        FROM ordinacija o
                 INNER JOIN ordinacija_specializacija os ON o.id = os.ordinacija_id
                 INNER JOIN specializacije s ON s.id = os.specializacija_id
                 INNER JOIN lokacija l ON l.id = o.lokacija_id
        WHERE o.ime ILIKE '%' || search_text || '%'
           OR l.ime ILIKE '%' || search_text || '%'
           OR s.ime ILIKE '%' || search_text || '%';
END;
$$ LANGUAGE plpgsql;



Spodnaj funkcija posodobi podatke od ordinacije
CREATE OR REPLACE FUNCTION update_ordinacija_data(
    ordinacija_name TEXT,
    new_ime TEXT,
    new_contact TEXT,
    new_working_hours TEXT,
    new_location_id INT,
    new_specialization_id INT
)
    RETURNS VOID AS $$
BEGIN
    -- Update the ordinacija table
    UPDATE ordinacija
    SET ime = new_ime,
        kontakt_osredotocen = new_contact,
        delovni_cas = new_working_hours,
        lokacija_id = new_location_id
    WHERE id = (SELECT id FROM ordinacija WHERE ime = ordinacija_name);


END;
$$ LANGUAGE plpgsql;


Spodnja funkcija dobi ID lokacije glede ne njeno ime
CREATE OR REPLACE FUNCTION get_location_id(location_name TEXT)
    RETURNS INT AS $$
DECLARE
    location_id INT;
BEGIN
    SELECT id INTO location_id
    FROM lokacija
    WHERE ime = location_name
    LIMIT 1;
    RETURN location_id;
END;
$$ LANGUAGE plpgsql;




Spodnja funkcija pridobi ID specializacije glede na njeno ime
CREATE OR REPLACE FUNCTION get_specialization_id(specialization_name TEXT)
    RETURNS INT AS $$
DECLARE
    specialization_id INT;
BEGIN
    SELECT id INTO specialization_id
    FROM specializacije
    WHERE ime = specialization_name
    LIMIT 1;
    RETURN specialization_id;
END;
$$ LANGUAGE plpgsql;



Spodnja funkcija mi omogoča, da izbrišem določene ordinacije iz baze. 
CREATE OR REPLACE FUNCTION delete_ordinacija(p_id INT)
    RETURNS VOID AS $$
BEGIN
    DELETE FROM ordinacija
    WHERE id = p_id;
END;
$$ LANGUAGE plpgsql;


ALTER TABLE ordinacija_specializacija
    DROP CONSTRAINT fk_ord_spec_ordinacija;

ALTER TABLE ordinacija_specializacija
    ADD CONSTRAINT fk_ord_spec_ordinacija
        FOREIGN KEY (ordinacija_id)
            REFERENCES ordinacija(id)
            ON DELETE CASCADE;
