#!/bin/sh

envsubst < /opt/keycloak/data/import/realm.template.json > /opt/keycloak/data/import/realm-user.json

/opt/keycloak/bin/kc.sh start --optimized --import-realm