#!/usr/bin/env bash
# docker run --name example1db -v /mysql-data:/var/lib/mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=tiger -e MYSQL_USER=scott -e MYSQL_PASSWORD=tiger -e MYSQL_DATABASE=example1 -d mysql/mysql-server
#
sudo rm -rf /mysql-data
sudo mkdir /mysql-data
sudo chown -c -R rodolfo /mysql-data
docker stop example1db
docker rm example1db
docker run --name example1db -v /mysql-data:/var/lib/mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=my-secret-pw -e MYSQL_DATABASE=example1db -d mysql/mysql-server:8.0.1
docker ps

# from inside mysql container
# grant all privileges on *.* to 'root'@'%'identified by 'my-secret-pw' ;
# flush privileges;
