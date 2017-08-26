SELECT '*** DATABASES:' as '' ;

SHOW databases ;

# use example1db

--GRANT ALL PRIVILEGES ON example1.* TO 'root' IDENTIFIED BY 'my-secret-pwd';
--GRANT ALL PRIVILEGES ON example1.* TO 'root'@'localhost' IDENTIFIED BY 'my-secret-pwd';
--GRANT ALL PRIVILEGES ON example1.* TO 'root'@'127.0.0.1' IDENTIFIED BY 'my-secret-pwd';
--
--CREATE USER 'lula_molusco' IDENTIFIED BY 'my-secret-pwd';
--GRANT ALL PRIVILEGES ON example1.* TO 'lula_molusco' IDENTIFIED BY 'my-secret-pwd';
--
--CREATE USER 'lula_molusco'@'localhost' IDENTIFIED BY 'my-secret-pwd';
--GRANT ALL PRIVILEGES ON example1.* TO 'lula_molusco'@'localhost' IDENTIFIED BY 'my-secret-pwd';
--
--CREATE USER 'lula_molusco'@'127.0.0.1' IDENTIFIED BY 'my-secret-pwd';
--GRANT ALL PRIVILEGES ON example1.* TO 'lula_molusco'@'127.0.0.1' IDENTIFIED BY 'my-secret-pwd';
--
--FLUSH PRIVILEGES;
