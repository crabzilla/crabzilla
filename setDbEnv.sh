#!/usr/bin/env bash

export WRITE_DATABASE_DRIVER=com.mysql.cj.jdbc.Driver
export WRITE_DATABASE_URL="jdbc:mysql://127.0.0.1:3306/example1_write?serverTimezone=UTC&useSSL=false"
export WRITE_DATABASE_USER=root
export WRITE_DATABASE_PASSWORD=my-secret-pwd
export WRITE_DATABASE_POOL_MAX_SIZE=7

export READ_DATABASE_DRIVER=com.mysql.cj.jdbc.Driver
export READ_DATABASE_URL="jdbc:mysql://127.0.0.1:3306/example1_read?serverTimezone=UTC&useSSL=false"
export READ_DATABASE_USER=root
export READ_DATABASE_PASSWORD=my-secret-pwd
export READ_DATABASE_POOL_MAX_SIZE=7
