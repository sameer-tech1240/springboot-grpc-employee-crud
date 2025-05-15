package com.grpc.crud.service;

import com.grpc.crud.*;
import com.grpc.crud.entity.EmployeeEntity;
import com.grpc.crud.exception.EmployeeNotFoundException;
import com.grpc.crud.repo.EmployeeRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@GrpcService
@RequiredArgsConstructor
public class EmployeeServiceImpl extends EmployeeServiceGrpc.EmployeeServiceImplBase {

    private final EmployeeRepository employeeRepository;

    /* convert EmployeeEntity ko EmployeeResponse */
    private EmployeeResponse mapToResponse(EmployeeEntity entity) {
        return EmployeeResponse.newBuilder()
                .setId(entity.getEmp_id())
                .setEmpName(entity.getEmp_name())
                .setEmpEmail(entity.getEmp_email())
                .setEmpAddress(entity.getEmp_address())
                .build();
    }

    @Override
    public void createEmployee(EmployeeRequest request, StreamObserver<EmployeeResponse> responseObserver) {
        try {
            EmployeeEntity employee = EmployeeEntity.builder()
                    .emp_name(request.getEmpName())
                    .emp_email(request.getEmpEmail())
                    .emp_address(request.getEmpAddress())
                    .build();

            employee = employeeRepository.save(employee);

            EmployeeResponse employeeResponse = mapToResponse(employee);
            responseObserver.onNext(employeeResponse);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Failed to create employee: " + ex.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void getEmployeeById(EmployeeIdRequest request, StreamObserver<EmployeeResponse> responseObserver) {
        try {
            EmployeeEntity entity = employeeRepository.findById(request.getId())
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee with ID " + request.getId() + " not found"));

            EmployeeResponse response = mapToResponse(entity);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EmployeeNotFoundException ex) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Something went wrong: " + e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void deleteEmployeeById(EmployeeIdRequest request, StreamObserver<DeleteEmployeeResponse> responseObserver) {
        try {
            EmployeeEntity entity = employeeRepository.findById(request.getId())
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found for deletion with ID " + request.getId()));

            employeeRepository.delete(entity);

            DeleteEmployeeResponse response = DeleteEmployeeResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Employee deleted successfully.")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (EmployeeNotFoundException ex) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException()
            );
        } catch (Exception ex) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Failed to delete employee: " + ex.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void getAllEmployees(Empty request, StreamObserver<EmployeeList> responseObserver) {
        try {
            List<EmployeeEntity> allEntities = employeeRepository.findAll();

            if (allEntities.isEmpty()) {
                throw new EmployeeNotFoundException("No employees found.");
            }

            List<EmployeeResponse> employees = allEntities.stream()
                    .map(this::mapToResponse)
                    .toList();

            EmployeeList list = EmployeeList.newBuilder()
                    .addAllEmployees(employees)
                    .build();

            responseObserver.onNext(list);
            responseObserver.onCompleted();

        } catch (EmployeeNotFoundException ex) {
            throw ex; // GlobalExceptionHandler handle karega
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch employees", ex);
        }
    }

    @Override
    public void updateEmployeeById(UpdateEmployeeRequest request, StreamObserver<EmployeeResponse> responseObserver) {
        try {
            EmployeeEntity entity = employeeRepository.findById(request.getId())
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with ID: " + request.getId()));

            // Update the fields
            entity.setEmp_name(request.getEmpName());
            entity.setEmp_email(request.getEmpEmail());
            entity.setEmp_address(request.getEmpAddress());

            EmployeeEntity updated = employeeRepository.save(entity);

            EmployeeResponse response = mapToResponse(updated);
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (EmployeeNotFoundException ex) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException()
            );
        } catch (Exception ex) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Failed to update employee: " + ex.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void getAllEmployeesStream(Empty request, StreamObserver<EmployeeResponse> responseObserver) {
        List<EmployeeEntity> employee = employeeRepository.findAll();
        employee.forEach(emp -> {
            EmployeeResponse response = EmployeeResponse.newBuilder()
                    .setId(emp.getEmp_id())
                    .setEmpName(emp.getEmp_name())
                    .setEmpEmail(emp.getEmp_email())
                    .setEmpAddress(emp.getEmp_address())
                    .build();

            responseObserver.onNext(response);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                responseObserver.onError(e);
                return;
            }
        });

        responseObserver.onCompleted();

    }

    @Override
    public void saveMultipleEmployees(EmployeeListRequest request, StreamObserver<UploadStatus> responseObserver) {
        List<EmployeeEntity> employeeList = request.getEmployeesList().stream().map(emp ->
                EmployeeEntity.builder()
                        .emp_name(emp.getEmpName())
                        .emp_email(emp.getEmpEmail())
                        .emp_address(emp.getEmpAddress())
                        .build()
        ).toList();

        employeeRepository.saveAll(employeeList);

        UploadStatus status = UploadStatus.newBuilder()
                .setSuccess(true)
                .setMessage(employeeList.size() + " employees saved successfully using batch insert.")
                .build();

        responseObserver.onNext(status);
        responseObserver.onCompleted();
    }


    @Override
    public StreamObserver<EmployeeIdRequest> getEmployeeByIdStream(StreamObserver<EmployeeResponse> responseObserver) {
        return new StreamObserver<EmployeeIdRequest>() {
            @Override
            public void onNext(EmployeeIdRequest request) {
                // Fetch each employee by ID
                employeeRepository.findById(request.getId()).ifPresentOrElse(emp -> {
                    EmployeeResponse response = EmployeeResponse.newBuilder()
                            .setId(emp.getEmp_id())
                            .setEmpName(emp.getEmp_name())
                            .setEmpEmail(emp.getEmp_email())
                            .setEmpAddress(emp.getEmp_address())
                            .build();
                    responseObserver.onNext(response);
                }, () -> {
                    // Employee not found
                    EmployeeResponse notFoundResponse = EmployeeResponse.newBuilder()
                            .setId(request.getId())
                            .setEmpName("Not Found")
                            .setEmpEmail("N/A")
                            .setEmpAddress("N/A")
                            .build();
                    responseObserver.onNext(notFoundResponse);
                });
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Client error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

}
