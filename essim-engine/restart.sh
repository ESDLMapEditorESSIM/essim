#!/bin/bash

sudo docker-compose pull essim-engine
sudo docker stack rm essim
while :
do
        sudo docker stack deploy --compose-file docker-compose.yml essim
        if [ $? -eq 0 ]
        then
                break
        fi
done
