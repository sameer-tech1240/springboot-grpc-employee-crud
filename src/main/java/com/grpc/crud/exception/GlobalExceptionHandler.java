package com.grpc.crud.exception;


import io.grpc.*;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;


@GrpcAdvice
public class GlobalExceptionHandler {

    @GrpcExceptionHandler(EmployeeNotFoundException.class)
    public StatusRuntimeException handleEmployeeNotFound(EmployeeNotFoundException ex) {
        return Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException();
    }

    // Optional: catch all other exceptions
    @GrpcExceptionHandler(Exception.class)
    public StatusRuntimeException handleAll(Exception ex) {
        return Status.INTERNAL.withDescription("Something went wrong: " + ex.getMessage()).asRuntimeException();
    }
}