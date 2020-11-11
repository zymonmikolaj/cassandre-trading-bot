package tech.cassandre.trading.bot.web.api.v1;

import org.reactivestreams.Publisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import tech.cassandre.trading.bot.dto.position.PositionDTO;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Position API.
 */
@RequestMapping(value = "/v1/positions", produces = APPLICATION_JSON_VALUE)
public interface PositionAPI {

    @GetMapping
    Publisher<PositionDTO> getAll();

    @GetMapping("/{id}")
    Publisher<PositionDTO> getById(@PathVariable("id") long id);

}
