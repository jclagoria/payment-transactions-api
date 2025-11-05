package com.merchant.transaction.application.command;

import com.merchant.transaction.application.command.dto.TransactionRequest;
import lombok.Value;

@Value
public class CreateTransactionCommand {

    TransactionRequest request;
    String idempotencyKey;

}
