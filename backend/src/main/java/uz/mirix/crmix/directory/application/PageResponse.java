package uz.mirix.crmix.directory.application;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
    public static <S, T> PageResponse<T> from(Page<S> source, java.util.function.Function<S, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.getNumber(),
                source.getSize());
    }
}
