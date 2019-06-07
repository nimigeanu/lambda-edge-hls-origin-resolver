'use strict';

//the name of the table holding the origin routing data
const DYNAMODB_TABLE = "originroute167";

//use a global variable to cache origins 
//if value not found or too old, it is looked upon in the database
var originCache;
//define the cache timeout (10 seconds)
var CACHE_TIMEOUT = 10 * 1000;

exports.handler = (event, context, callback) => {
 if (!originCache) {
   originCache = {};
 }
 //console.log("originCache: " + originCache);
 const request = event.Records[0].cf.request;
 const uri = request.uri;
 //console.log("uri: " + uri);
 let split = uri.split("/");
 if (split.length > 2){
  let streamName = split[split.length - 2];
  console.log("streamName: " + streamName);
  
  let now = new Date();
  if (originCache[streamName]){
    //console.log("got cached item for " + streamName);
    let cacheItem = originCache[streamName];
    let lastUpdated = cacheItem.time;
    let timeout = now - lastUpdated;
    //console.log("timeout: " + timeout);
    if (timeout < CACHE_TIMEOUT){
      //console.log("using cached value: " + cacheItem.origin);
      setRequestOrigin(request, cacheItem.origin);
      callback(null, request);
      return;
    }
    else {
      //delete expired value
      delete originCache[streamName];
    }
  }
  
  var AWS = require('aws-sdk');
  // Set the region 
  AWS.config.update({region: 'us-east-1'});

  // Create the DynamoDB service object
  var ddb = new AWS.DynamoDB({apiVersion: '2012-08-10'});

  var params = {
    'TableName': DYNAMODB_TABLE,
    'Key': {
      'Stream': {S: streamName}
    },
    'ProjectionExpression': 'Origin'
  };
  
  // Call DynamoDB to read the item from the table
  ddb.getItem(params, function(err, data) {
    if (err) {
      //console.log("Query Error", err);
      var response = setError(err);
      callback(null, response);
    } else {
      //console.log("Query Success", data.Item);
      if (data.Item){
        let origin = data.Item.Origin.S;
        //console.log("origin: ", origin);
        originCache[streamName] = {origin: origin, time:new Date()};
        setRequestOrigin(request, origin);
        callback(null, request);
      }
      else {
        var response = setNotFound("No origin for " + streamName);
        callback(null, response);
      }
    }
    
  });
 }
 else {
    var response = setError("Malformed uri: " + request.uri);
    callback(null, response);
 }
};

function setRequestOrigin(request, originHost){
  request.origin = {
    custom: {
      domainName: originHost,
      port: 80,
      protocol: 'http',
      sslProtocols: ['TLSv1', 'TLSv1.1'],
      path: '',
      readTimeout: 5,
      keepaliveTimeout: 5,
      customHeaders: {}
    }
  };
  request.headers['host'] = [{ key: 'host', value: originHost}];
}

function setNotFound(text){
  const content = `
    <\!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="utf-8">
        <title>Not Found :(</title>
      </head>
      <body>
        <p>${text}</p>
      </body>
    </html>
  `;
  var response = {
    body: content,
    status: '404',
    statusDescription: "Not Found"
  };
  return response;
}

function setError(text){
  const content = `
    <\!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="utf-8">
        <title>Internal Error :(</title>
      </head>
      <body>
        <p>${text}</p>
      </body>
    </html>
  `;
  var response = {
    body: content,
    status: '502',
    statusDescription: "Internal Server Error"
  };
  return response;
}