@echo off
echo Stopping edufy-gateway-service
docker stop edufy-gateway-service

echo Deleting container edufy-gateway-service
docker rm edufy-gateway-service

echo Deleting image edufy-gateway-service
docker rmi edufy-gateway-service

echo Running mvn package
call mvn package

echo Creating image edufy-gateway-service
docker build -t edufy-gateway-service .

echo Creating and running container edufy-gateway-service
docker run -d -p 4505:4505 --name edufy-gateway-service --network cadett-splitters-net edufy-gateway-service

echo Done!