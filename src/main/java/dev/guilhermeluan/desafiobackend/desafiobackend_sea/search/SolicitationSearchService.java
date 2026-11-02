package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.Coverage;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.CoverageGuard;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos.PageResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos.SolicitationSearchItem;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos.SolicitationSearchParams;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class SolicitationSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final CoverageGuard coverageGuard;

    public SolicitationSearchService(ElasticsearchOperations elasticsearchOperations, CoverageGuard coverageGuard) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.coverageGuard = coverageGuard;
    }

    private static List<Query> coverageFilter(Coverage coverage) {
        if (coverage.isUnrestricted()) {
            return List.of();
        }
        return List.of(termsIn("state", List.copyOf(coverage.ufs())));
    }

    private static List<Query> userFilters(SolicitationSearchParams params) {
        List<Query> filters = new ArrayList<>();
        if (params.status() != null) {
            filters.add(term("status", params.status().name()));
        }
        if (params.serviceType() != null) {
            filters.add(term("serviceType", params.serviceType().name()));
        }
        if (params.priority() != null) {
            filters.add(term("priority", params.priority().name()));
        }
        if (params.state() != null) {
            filters.add(term("state", params.state()));
        }
        if (params.dateFrom() != null || params.dateTo() != null) {
            filters.add(submittedBetween(params.dateFrom(), params.dateTo()));
        }
        return filters;
    }

    private static List<Query> textQuery(SolicitationSearchParams params) {
        if (params.q() == null) {
            return List.of();
        }
        return List.of(Query.of(q -> q.multiMatch(m -> m.query(params.q()).fields("title^2", "description"))));
    }

    private static Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    private static Query termsIn(String field, List<String> values) {
        List<FieldValue> fieldValues = values.stream().map(FieldValue::of).toList();
        return Query.of(q -> q.terms(t -> t.field(field).terms(terms -> terms.value(fieldValues))));
    }

    private static Query submittedBetween(LocalDate from, LocalDate to) {
        return Query.of(q -> q.range(r -> r.date(d -> {
            d.field("submittedAt").format("strict_date_optional_time");
            if (from != null) {
                d.gte(startOfDay(from));
            }
            if (to != null) {
                d.lt(startOfDay(to.plusDays(1)));
            }
            return d;
        })));
    }

    private static String startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
    }

    private static Sort sortOf(SolicitationSearchParams params) {
        return Sort.by(params.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, params.sortField());
    }

    public PageResponse<SolicitationSearchItem> search(String email, Role role, SolicitationSearchParams params) {
        Coverage coverage = coverageGuard.coverageOf(email, role);

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(b -> b
                        .mustNot(term("status", "DRAFT"))
                        .filter(coverageFilter(coverage))
                        .filter(userFilters(params))
                        .must(textQuery(params)))))
                .withPageable(PageRequest.of(params.page(), params.size(), sortOf(params)))
                .withTrackTotalHits(true)
                .build();

        SearchHits<SolicitationDocument> hits = elasticsearchOperations.search(query, SolicitationDocument.class);

        List<SolicitationSearchItem> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(SolicitationSearchItem::from)
                .toList();

        return new PageResponse<>(items, params.page(), params.size(), params.sort(), hits.getTotalHits());
    }
}
