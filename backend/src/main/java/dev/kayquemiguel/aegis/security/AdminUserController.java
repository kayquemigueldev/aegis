
package dev.kayquemiguel.aegis.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(
            AdminUserService service
    ) {
        this.service = service;
    }

    public record CreateAnalystRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotNull Set<String> sensors
    ) {
    }

    public record ReplaceSensorsRequest(
            @NotNull Set<String> sensors
    ) {
    }

    public record SetEnabledRequest(
            @NotNull Boolean enabled
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword
    ) {
    }

    @GetMapping("/users")
    public List<AdminUserService.UserView> listUsers() {
        return service.listUsers();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserService.UserView createAnalyst(
            @Valid @RequestBody CreateAnalystRequest request
    ) {
        return service.createAnalyst(
                request.username(),
                request.password(),
                request.sensors()
        );
    }

    @PutMapping("/users/{id}/sensors")
    public AdminUserService.UserView replaceSensors(
            @PathVariable UUID id,
            @Valid @RequestBody ReplaceSensorsRequest request
    ) {
        return service.replaceSensors(
                id,
                request.sensors()
        );
    }

    @PatchMapping("/users/{id}/enabled")
    public AdminUserService.UserView setEnabled(
            @PathVariable UUID id,
            @Valid @RequestBody SetEnabledRequest request
    ) {
        return service.setAnalystEnabled(
                id,
                request.enabled()
        );
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        service.changeOwnPassword(
                authentication.getName(),
                request.currentPassword(),
                request.newPassword()
        );
    }
}
