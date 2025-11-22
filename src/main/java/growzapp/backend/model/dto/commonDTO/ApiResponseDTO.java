package growzapp.backend.model.dto.commonDTO;

public record ApiResponseDTO<T>(
        boolean success,
        String message,
        T data) {

    public static <T> ApiResponseDTO<T> success(T data) {
        return new ApiResponseDTO<>(true, "Opération réussie", data);
    }

    public static <T> ApiResponseDTO<T> error(String message) {
        return new ApiResponseDTO<>(false, message, null);
    }

    // Méthode fluide pour changer le message
    public ApiResponseDTO<T> message(String newMessage) {
        return new ApiResponseDTO<>(this.success, newMessage, this.data);
    }
  
}