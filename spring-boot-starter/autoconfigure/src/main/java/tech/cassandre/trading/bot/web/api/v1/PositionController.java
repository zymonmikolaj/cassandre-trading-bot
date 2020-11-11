package tech.cassandre.trading.bot.web.api.v1;

import org.reactivestreams.Publisher;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.cassandre.trading.bot.dto.position.PositionDTO;
import tech.cassandre.trading.bot.service.PositionService;

import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Position API controller.
 */
@RestController
public class PositionController implements PositionAPI {

    /** Position service. */
    private final PositionService positionService;

    /**
     * Constructor.
     *
     * @param newPositionService position service
     */
    public PositionController(final PositionService newPositionService) {
        this.positionService = newPositionService;
    }

    @Override
    public final Publisher<PositionDTO> getAll() {
        return Flux.fromStream(positionService.getPositions().stream());
    }

    @Override
    public final Publisher<PositionDTO> getById(final long id) {
        final Optional<PositionDTO> p = positionService.getPositionById(id);
        if (p.isPresent()) {
            return Mono.just(p.get());
        } else {
            throw new ResponseStatusException(NOT_FOUND, "Position " + id + " not found");
        }
    }

}
