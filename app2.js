const express = require('express'),
    http = require('http'),
    path = require('path'),
    assert = require('assert'),
    fs = require('fs'),
    mongodb = require('mongodb'),
    util = require('util'),
    bodyParser = require('body-parser')
const bankLog = util.debuglog('banking');
let REST_COBURL = '/api/conlinebank';
let traceStr = process.env.NODE_DEBUG;
let bankTrace = (traceStr.indexOf("bank") >= 0),
    verbose = (traceStr.indexOf("verbose") >= 0);

let url = "mongodb://127.0.0.1:27017/",
    dbnm = "conLineBank",
    MongoClient = mongodb.MongoClient,
    mongoDbConnectionPool = null;

var app = express();
app.use(bodyParser.json()); // support json encoded bodies
app.use(bodyParser.urlencoded({ extended: true }));

app.set('port', process.env.BANKPORT || 3000);
app.use(express.static(__dirname + '/public'));
if (bankTrace) bankLog("MC: created connection pool: " + REST_COBURL);

/*********************************************************************************************************************************
  Retrieve names of collections
*********************************************************************************************************************************/
app.get(REST_COBURL + '/listCollections', function(request, response) {
    if (verbose) bankLog("Called listCollections");
    getMongoDbConnection(url, dbnm)
        .then(dbConn => {
            console.log("Came into then dbConn from getConnection");
            dbConn.collection("myNewCollection1").findOne({})
                .then(docs => {
                    console.log("into docs from find");
                    let resultStr = JSON.stringify(docs);
                    console.log("Row: %s", resultStr);
                    response.write(prepareResponse(docs, null));
                    response.end();
                })
                //            dbConn.close();
        })
        .catch(err => {
            if (err !== null && err.length !== 0) console.log("Error is not null for connection: %s", err);
            else console.log("Null or empty err");
            var errString = "Failure to get data: " + JSON.stringify(err);
            bankLog("Error: %s", errString);
            response.write(prepareResponse(null, err));
            response.end();
        });
});

function getMongoDbConnection(url, dbName) {

    if (mongoDbConnectionPool && mongoDbConnectionPool.isConnected(dbName)) {
        console.log('Reusing the connection from pool');
        return Promise.resolve(mongoDbConnectionPool.db(dbName));
    }

    console.log('Init the new connection pool');
    return MongoClient.connect(url, { poolSize: 10 })
        .then(dbConnPool => {
            console.log('Had to do connection with url: %s and dbName: %s', url, dbName);
            mongoDbConnectionPool = dbConnPool;
            return mongoDbConnectionPool.db(dbName);
        });
}

function prepareResponse(result, err) {
    if (err) return JSON.stringify({ statusCode: 500, body: err });
    else return JSON.stringify({ statusCode: 200, body: result });
}

/*********************************************************************************************************************************
 Start the express appServer going
*********************************************************************************************************************************/
http.createServer(app).listen(app.get('port'), function() {
    bankLog('App:createServer Express server listening on port %s', app.get('port'));
});