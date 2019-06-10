# Multiple Origin HLS streaming with Wowza and CloudFront

## Specifics
* HLS calls (manifest and payload) are routed to the proper origin via Lambda@Edge
* Mechanism relies on a DynamoDb table that holds origin information for the active streams
* Edge resolution has 2 cache levels, making this extremely fast
* Fully scalable

## Setup

### DynamoDb 
Create a DynamoDb table with the following characteristics
* Table name `originroute167`
* Primary partition key `Stream (String)`
* Read/write capacity mode `On-Demand`

### IAM role
Create an IAM role with the following characteristics
* Name: `origin-route-167`
* Permissions policies: `AmazonDynamoDBFullAccess` (for tighter security only give it access to table created above)

### Lambda
Create a Lambda function with the following characteristics
* Function name `originRoute167`
* Code [lambda/index.js](lambda/index.js)
* Runtime `Node.js 8.10`
* Timeout `10 seconds`
* Execution role: `origin-route-167`

### CloudFront
Create a CloudFront distribution with the following characteristics 
* Delivery Method	`Web`
* Origin Domain Name `originroute167.com` (arbitrary - will be overwritten by Lambda)
* Lambda Function Associations
  * Event Type `Origin Request` 
  * Lambda Function ARN: ARN of `origin-route-167` function created above
  * Include Body `No`
* Error Pages
  * Not Found
    * HTTP Error Code `404`
    * Error Caching Minimum TTL `5`
  * Bad Gateway
    * HTTP Error Code `502`
    * Error Caching Minimum TTL `5`
  * Gateway Timeout
    * HTTP Error Code `504`
    * Error Caching Minimum TTL `5`

### Wowza

Apply the following setup to every Wowza origin (or to the AMI/startup pack if you have autoscale):

1. upload the provided `wowza/lib/*` files to your wowza `lib` folder
2. create a new *Live HTTP Origin* application (or modify the existing)
3. add module `com.client167.modules.NotifyOriginModule` to your application
4. add the following custom properties to your application:
	* `originRoutingTableName` - name of the DynamoDB table created above
	* `originHostname`(if not running in AWS) - IP or DNS-valid host name of the origin server, for the edges to connect to; if running your Wowza in AWS you can omit this as it will be detected automatically