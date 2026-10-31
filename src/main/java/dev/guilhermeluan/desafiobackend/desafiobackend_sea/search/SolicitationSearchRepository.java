package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SolicitationSearchRepository extends ElasticsearchRepository<SolicitationDocument, Long> {
}
