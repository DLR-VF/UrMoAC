# Testing UrMoAC

UrMoAC is tested using the acceptance tests framework [TextTest](http://texttest.sourceforge.net/).

To run the tests

* Install [TextTest](http://texttest.sourceforge.net/)
* Export an executable jar to ___&lt;URMOAC&gt;_\bin\UrMoAC.jar__
* execute ___&lt;URMOAC&gt;_\tests\runUrMoACTests.bat__

To run [PostgreSQL](https://www.postgresql.org/) + [PostGIS](https://postgis.net/) tests you additionally have to

* build a local (at localhost) database named `urmoac_postgres_tests`
* add [PostGIS](https://postgis.net/) extensions (`CREATE EXTENSION postgis;`)
* add a user named `urmoactests` with `urmoactests` as password (`create user urmoactests with encrypted password 'urmoactests'`)
* give the user access to the database (`GRANT ALL PRIVILEGES ON DATABASE urmoac_postgres_tests TO urmoactests;`)
* execute ___&lt;URMOAC&gt;_\tests\runUrMoACPostgresTests.bat__




