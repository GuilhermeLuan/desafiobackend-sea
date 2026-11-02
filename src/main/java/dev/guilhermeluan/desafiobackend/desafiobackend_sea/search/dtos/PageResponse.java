package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, String sort, long total) {
}
