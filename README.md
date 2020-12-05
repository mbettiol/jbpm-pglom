# jbpm-pglom
***

jbpm-plglom is an sql script generator that prevents you from loosing process data (including active processes) and running out of space when using jBPM 6 or 7 on top of PostgreSQL.

jBPM with PostgreSQL uses [pg_largeobjects](https://www.postgresql.org/docs/10/largeobjects.html) to store binary data: columns referencing binary data should match type "oid" but this is not the case for all jBPM columns unfortunately.

Not using the proper column type "oid" will cause data loss when running [vacuumlo](https://www.postgresql.org/docs/10/vacuumlo.html) because PostgreSQL assume the binary data are unreferenced.

Running vacuumlo is the only OOTB way recover space allocated by unreferenced binary data and is a mandatory maintenance activity, especially on busy systems, in order to avoid running out-of-space.


## How It Works

2 types of triggers play together to prevent data loss and allow postgres to recover unreferenced storage as soon as possible:

- For each column of the wrong type a new column of type "oid" is created on the same table and a trigger updates the shadow field to prevent data loss
- On update or delete of rows with "oid" another trigger performs the [lo_unlink](https://www.postgresql.org/docs/10/lo-interfaces.html) synchronously marking the largeobject as "unreferenced"


## Supported jBPM Versions

The generated script was tested on jBPM 6.4/6.5 and jBPM 7 

### Recent JBPM Versions

All jBPM 6 and 7 are affected. <br>
The [OOTB RH-solution](https://github.com/tkobayas/jbpm-postgresql-lo-trigger-gen/blob/master/src/main/java/com/redhat/gss/jbpm/PostgreSQLLOCreateTriggerGen.java) does not cover immediate space deallocation and introduces additional tables managed by triggers amplifying the database load.<br>
A more recent solution seems provided with [JBPM-9264](https://issues.redhat.com/browse/JBPM-9264) but is not enabled by default. 
 
## Usage

Choose the jbpm version (JBPM6 or JBPM7) and a prefix for the generated columns, functions and triggers.

Scripts will be printed on console

```
java -jar pg-large-object-maintenance-0.0.1-jar-with-dependencies.jar -j=JBPM6 -p=acme
```

example

```
/*
 *  i18ntext.text
 */
 
ALTER TABLE i18ntext ADD COLUMN acme_oid_text oid;

CREATE FUNCTION acme_oid_set$i18ntext$text() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF (TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN 
		NEW.acme_oid_text = NEW.text::oid;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER acme_oid_set$i18ntext$text$trg 
	BEFORE INSERT OR UPDATE ON i18ntext 
	FOR EACH ROW EXECUTE PROCEDURE acme_oid_set$i18ntext$text();

CREATE FUNCTION acme_oid_unlink$i18ntext() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	IF (TG_OP = 'UPDATE') THEN
        IF(OLD.acme_oid_text IS NOT NULL AND OLD.acme_oid_text IS DISTINCT FROM NEW.acme_oid_text) THEN
                PERFORM lo_unlink(OLD.acme_oid_text);
        END IF;
        RETURN NEW;
    ELSEIF (TG_OP = 'DELETE') THEN
        IF (OLD.acme_oid_text is not null) THEN 
        	PERFORM lo_unlink(OLD.acme_oid_text);
        END IF;
        RETURN OLD;
    ELSE
    	RAISE EXCEPTION 'Trigger OP must be ''UPDATE'' or ''DELETE''';
    END IF;
END;
$$;

CREATE TRIGGER acme_oid_unlink$i18ntext$trg 
  AFTER UPDATE OR DELETE 
  ON i18ntext
  FOR EACH ROW EXECUTE 
  PROCEDURE acme_oid_unlink$i18ntext();
```
