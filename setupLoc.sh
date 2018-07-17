#!/bin/bash
echo "Setting up development environment for BlockChain"
export NODE_DEBUG=banking,verbose,credit,municipal
#export NODE_DEBUG=general
export PORT=8080
export CLIHOST=localhost
export CLIPORT=8080
export BANKHOST=localhost
export BANKPORT=8081
export CREDITHOST=localhost
export CREDITPORT=8082
export DEEDHOST=localhost
export DEEDPORT=8083
export VCAP_SERVICES='{ "cloudantNoSQLDB": [ { "name": "DreamCarZ-cloudantNoSQLDB", "label": "cloudantNoSQLDB", "plan": "Shared", "credentials": { "username": "3e0f51ef-f543-46c7-bbb5-dbd6fcdc2e95-bluemix", "password": "676c77a8e5eebfbcaa7707632e75c53daed925f537a85b910b04a9525f98cd72", "host": "3e0f51ef-f543-46c7-bbb5-dbd6fcdc2e95-bluemix.cloudant.com", "port": 443, "url": "https://3e0f51ef-f543-46c7-bbb5-dbd6fcdc2e95-bluemix:676c77a8e5eebfbcaa7707632e75c53daed925f537a85b910b04a9525f98cd72@3e0f51ef-f543-46c7-bbb5-dbd6fcdc2e95-bluemix.cloudant.com" } } ], "personality_insights": [ { "name": "Personality Insights-26", "label": "personality_insights", "plan": "tiered", "credentials": { "url": "https://gateway.watsonplatform.net/personality-insights/api", "password": "sm1lYCOs4eEE", "username": "2f06d185-aef6-4d4a-94e9-864eb30ff579" } } ] }'

echo "All done - now just run node start to run the application"
