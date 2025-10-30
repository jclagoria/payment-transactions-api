# Merchant Transactions API

## Objective

The goal of this assignment is to build an API to manage our merchants' payment transactions. Implement the assignment
using any programming language or framework of your choice. Feel free to use the tools and
technologies you are most comfortable with.

### Provided Resources

- We have provided an API using [json-server](https://github.com/typicode/json-server) that already includes endpoints
  to manage transactions and receivables. You
  can also use json-server as a database for development purposes. **If you're unfamiliar with json-server, please take
  some time to explore its functionalities before proceeding with the assignment**.
- An API for the Numerator service to generate unique IDs.

### Task: Create Merchant Transactions API

The goal of this assignment is to build an API that processes and stores transactions initiated by a merchant, ensuring correct
fee deduction and the creation of corresponding receivables.

A created transaction must include:

- A unique ID created using Numerator API.
- The total transaction amount, formatted as a decimal string.
- A description of the transaction, for example, "T-Shirt Black M".
- Payment method: **debit_card** or **credit_card**.
- The card number (only the last 4 digits should be stored and returned, as it is sensitive information).
- The name of the cardholder.
- Card expiration date in MM/YY format.
- Card CVV.

When creating a transaction, **a merchant receivable must also be created**, a receivable represents the portion
of the transaction amount that goes to the merchant after deducting the applicable fee.

#### Rules for Creating Receivables

| Transaction Type | Receivable Status | Payment Date                     | Fee |
|------------------|-------------------|----------------------------------|-----|
| **Debit Card**   | `paid`            | Same as creation date (D + 0)    | 2%  |
| **Credit Card**  | `waiting_funds`   | Creation date + 30 days (D + 30) | 4%  |

**Example**: If a receivable is created with a value of ARS 100.00 from a transaction with a **credit_card**, the
merchant will receive ARS 96.00.

### Unique ID Generation with Numerator API

It is essential that **_transactions and receivables_** have unique IDs generated. The **numerator** service
simulates an external system that helps you implement your own ID generation logic.

**Note**: The numerator API returns a number for the generated IDs but json-server expects strings for IDs, you'll need
to convert the number into a string format.

## Setup

### Start provided services

```
docker compose up
```

This will expose:

1. In http://0.0.0.0:8080/ the API for managing transactions and receivables
2. In http://0.0.0.0:3000/ the API for ids generation.

## API Services Overview

### Transactions

| Endpoint           | Method   | Description                                                         | Request Body                                                                                                                                                                                            |
|--------------------|----------|---------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `transactions`     | `GET`    | List all transactions.                                              | -                                                                                                                                                                                                       |
| `transactions/:id` | `GET`    | Get details of a specific transaction by ID.                        | -                                                                                                                                                                                                       |
| `transactions`     | `POST`   | Create a new transaction. Use numerator-api to generate a unique ID. | `{ "id": <string>, "value": "250.00", "description": "T-Shirt", "method": "credit_card", "cardNumber": "2222", "cardHolderName": "Simplenube Store", "cardExpirationDate": "04/28", "cardCvv": "222" }` |
| `transactions/:id` | `DELETE` | Delete a transaction by ID.                                         | -                                                                                                                                                                                                       |

### Receivables

| Endpoint          | Method   | Description                                                        | Request Body                                                                                                                                   |
|-------------------|----------|--------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `receivables`     | `GET`    | List all receivables.                                              | -                                                                                                                                              |
| `receivables/:id` | `GET`    | Get details of a specific receivable by ID.                        | -                                                                                                                                              |
| `receivables`     | `POST`   | Create a new receivable. Use numerator-api to generate a unique ID. | `{ "id": <string>, "status": "waiting_funds", "create_date": "2022-05-20T19:20:14.576-03:00", "subtotal": 250, "discount": 10, "total": 240g }` |
| `receivables/:id` | `DELETE` | Delete a receivable by ID.                                         | -                                                                                                                                              |

### Numerator

Numerator is a service that provides unique IDs, which are required for creating transactions and receivables. Although
the service offers several endpoints, you’re not required to use all of them. The implementation can be done in various ways
depending on your approach to generating unique IDs.

| Endpoint                 | Method   | Description                                                                                                                                                                                                                                                                                                                                                | Request Body                                     |
|--------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| `numerator`              | `GET`    | Retrieves the current numerator value, regardless of the lock status. Always returns the current value, even if the repository is locked.                                                                                                                                                                                                                  | -                                                |
| `numerator`              | `PUT`    | Sets the numerator value to the specified `value` immediately, without checking if the repository is locked.                                                                                                                                                                                                                                               | `{ "value": <number> }`                          |
| `numerator/test-and-set` | `PUT`    | Atomically sets the numerator to `newValue` if the current value matches `oldValue`. Returns `newValue` on success, or -1 if it fails. This operation is atomic, ensuring that the comparison and setting of the new value cannot be interrupted by other operations.                                                                                      | `{ "oldValue": <number>, "newValue": <number> }` |
| `numerator/lock`         | `POST`   | Sets the lock flag (`lock = true`) on the numerator repository, blocking it until the numerator can be accessed. There is a timeout parameter which is the amount of time the system will keep trying to acquire the lock (default timeout is 10,000 milliseconds or 10 seconds) - it does NOT release the lock. It throws an exception on timeout. Only one request can hold the lock at a time, and the lock is not automatically released when the request finishes processing. | `{ "timeout": <number, in milliseconds> }`                        |
| `numerator/lock`         | `DELETE` | Releases the lock by setting the lock flag to `false`. If it's already `false`, it remains unchanged.                                                                                                                                                                                                                                                      | -                                                |

## Bonus Track (Optional)

**If time permits**, consider implementing the following bonus tasks, while **these tasks are optional**, completing
them will be viewed favorably in the evaluation process.

### **Calculate Total Receivables per Period**

Develop functionality to calculate the merchant's total receivables per period. The response should include:

- Total amount of receivables.
- Amount receivable in the future.
- Total fee charged.

### **List All Merchant Transactions**

Create an endpoint that returns all transactions for a given merchant.
