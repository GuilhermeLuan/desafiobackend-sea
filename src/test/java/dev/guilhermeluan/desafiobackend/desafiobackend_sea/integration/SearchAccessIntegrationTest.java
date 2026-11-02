package dev.guilhermeluan.desafiobackend.desafiobackend_sea.integration;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.SolicitationDocument;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchAccessIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private static String newToken() {
        return "tok" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private long solicitation(String clientToken, String cep, String token, boolean submit) throws Exception {
        long id = createDraft(clientToken);
        send(put("/solicitations/" + id + "/steps/1"), clientToken,
                "{\"serviceType\":\"INSTALLATION\",\"title\":\"" + token + " instalação\","
                        + "\"description\":\"Descrição padrão com mais de vinte caracteres\"}").andExpect(status().isOk());
        send(put("/solicitations/" + id + "/steps/2"), clientToken, address(cep, "10")).andExpect(status().isOk());
        send(put("/solicitations/" + id + "/steps/3"), clientToken, CONFIRMATION).andExpect(status().isOk());
        if (submit) {
            send(post("/solicitations/" + id + "/submit"), clientToken).andExpect(status().isOk());
        }
        return id;
    }

    private ResultActions search(String token, String query) throws Exception {
        elasticsearchOperations.indexOps(SolicitationDocument.class).refresh();
        return send(get("/analyst/solicitations/search?" + query), token);
    }

    private static List<Long> idsOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$.items[*].id");
        return ids.stream().map(Number::longValue).sorted().toList();
    }

    private static List<Long> sorted(Long... ids) {
        return List.of(ids).stream().sorted().toList();
    }

    @Test
    void anAnalystOnlySeesTheUfsOfTheirCoverage() throws Exception {
        String clientToken = registerClientAndLogin();
        String token = newToken();
        long inSp = solicitation(clientToken, SP_CEP, token, true);
        long inDf = solicitation(clientToken, BSB_CEP, token, true);

        assertEquals(sorted(inSp), idsOf(search(createAnalystAndLogin("SP"), "q=" + token)));
        assertEquals(sorted(inDf), idsOf(search(createAnalystAndLogin("DF"), "q=" + token)));
        assertEquals(sorted(inSp, inDf), idsOf(search(createAnalystAndLogin("SP", "DF"), "q=" + token)));
        search(createAnalystAndLogin("RJ"), "q=" + token).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void theStateParameterCanOnlyNarrowAnAnalystsCoverageNeverWidenIt() throws Exception {
        String clientToken = registerClientAndLogin();
        String token = newToken();
        long inSp = solicitation(clientToken, SP_CEP, token, true);
        solicitation(clientToken, BSB_CEP, token, true);
        String spAnalyst = createAnalystAndLogin("SP");

        search(spAnalyst, "q=" + token + "&state=DF").andExpect(jsonPath("$.total").value(0));
        assertEquals(sorted(inSp), idsOf(search(spAnalyst, "q=" + token + "&state=SP")));
    }

    @Test
    void anAnalystNeverSeesDraftsEvenWhenCompleteAndInACoveredUf() throws Exception {
        String clientToken = registerClientAndLogin();
        String token = newToken();
        solicitation(clientToken, SP_CEP, token, false);
        long submitted = solicitation(clientToken, SP_CEP, token, true);
        String analystToken = createAnalystAndLogin("SP");

        assertEquals(sorted(submitted), idsOf(search(analystToken, "q=" + token)));
        search(analystToken, "q=" + token + "&status=DRAFT").andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void anAdminSearchesEveryUfButStillNeverSeesDrafts() throws Exception {
        String clientToken = registerClientAndLogin();
        String token = newToken();
        long inSp = solicitation(clientToken, SP_CEP, token, true);
        long inDf = solicitation(clientToken, BSB_CEP, token, true);
        solicitation(clientToken, SP_CEP, token, false);

        assertEquals(sorted(inSp, inDf), idsOf(search(adminToken, "q=" + token)));
        search(adminToken, "q=" + token + "&status=DRAFT").andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void theStateFilterStillNarrowsAnAdminSearch() throws Exception {
        String clientToken = registerClientAndLogin();
        String token = newToken();
        solicitation(clientToken, SP_CEP, token, true);
        long inDf = solicitation(clientToken, BSB_CEP, token, true);

        assertEquals(sorted(inDf), idsOf(search(adminToken, "q=" + token + "&state=DF")));
        search(adminToken, "q=" + token + "&state=RJ").andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void clientsCannotSearchButAnAdminCan() throws Exception {
        send(get("/analyst/solicitations/search"), registerClientAndLogin()).andExpect(status().isForbidden());
        send(get("/analyst/solicitations/search"), adminToken).andExpect(status().isOk());
        mockMvc.perform(get("/analyst/solicitations/search")).andExpect(status().isUnauthorized());
    }
}
