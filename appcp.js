'use strict'

var MongoClient = require('mongodb').MongoClient;

let mongoDbConnectionPool = null;
let scalegridMongoURI = null;
let scalegridMongoDbName = null;

exports.handler = (event, context, callback) => {

    console.log('Received event:', JSON.stringify(event));
    console.log('remaining time =', context.getRemainingTimeInMillis());
    console.log('functionName =', context.functionName);
    console.log('AWSrequestID =', context.awsRequestId);
    console.log('logGroupName =', context.logGroupName);
    console.log('logStreamName =', context.logStreamName);
    console.log('clientContext =', context.clientContext);
   
    // This freezes node event loop when callback is invoked
    context.callbackWaitsForEmptyEventLoop = false;

    var mongoURIFromEnv = process.env['SCALEGRID_MONGO_CLUSTER_URI'];
    var mongoDbNameFromEnv = process.env['SCALEGRID_MONGO_DB_NAME'];
    if(!scalegridMongoURI) {
    if(mongoURIFromEnv){
        scalegridMongoURI = mongoURIFromEnv;
    } else {
        var errMsg = 'Scalegrid MongoDB cluster URI is not specified.';
        console.log(errMsg);
        var errResponse = prepareResponse(null, errMsg);
        return callback(errResponse);
    }           
    }

    if(!scalegridMongoDbName) {
    if(mongoDbNameFromEnv) {
                scalegridMongoDbName = mongoDbNameFromEnv;
    } else {
        var errMsg = 'Scalegrid MongoDB name not specified.';
        console.log(errMsg);
        var errResponse = prepareResponse(null, errMsg);
        return callback(errResponse);
    }
    }

    handleEvent(event, context, callback);
};


function getMongoDbConnection(uri) {

    if (mongoDbConnectionPool && mongoDbConnectionPool.isConnected(scalegridMongoDbName)) {
        console.log('Reusing the connection from pool');
        return Promise.resolve(mongoDbConnectionPool.db(scalegridMongoDbName));
    }

    console.log('Init the new connection pool');
    return MongoClient.connect(uri, { poolSize: 10 })
        .then(dbConnPool => { 
                            mongoDbConnectionPool = dbConnPool; 
                            return mongoDbConnectionPool.db(scalegridMongoDbName); 
                          });
}

function handleEvent(event, context, callback) {
    getMongoDbConnection(scalegridMongoURI)
        .then(dbConn => {
            console.log('retrieving userId from event.pathParameters');
            var userId = event.pathParameters.userId;
            getAlertForUser(dbConn, userId, context);
        })
        .then(response => {
            console.log('getAlertForUser response: ', response);
            callback(null, response);
        })
        .catch(err => {
            console.log('=> an error occurred: ', err);
            callback(prepareResponse(null, err));
        });
}

function getAlertForUser(dbConn, userId, context) {

    return dbConn.collection('useralerts').find({'userId': userId}).sort({$natural:1}).limit(1)
        .toArray()
        .then(docs => { return prepareResponse(docs, null);})
        .catch(err => { return prepareResponse(null, err); });
}

function prepareResponse(result, err) {
    if(err) {
        return { statusCode:500, body: err };
    } else {
        return { statusCode:200, body: result };
    }
}
