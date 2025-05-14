package com.grpc.crud.service;

import com.grpc.crud.*;
import com.grpc.crud.entity.EmployeeEntity;
import com.grpc.crud.exception.EmployeeNotFoundException;
import com.grpc.crud.repo.EmployeeRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

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
}
