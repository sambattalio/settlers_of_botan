package bot;

import bot.decision.DecisionTreeDM;
import bot.decision.DecisionTreeType;
import soc.debug.D;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.robot.DiscardStrategy;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCRobotBrain;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static soc.game.SOCResourceConstants.*;
import static soc.robot.SOCPossiblePiece.*;

public class NDDiscardStrategy extends DiscardStrategy {
    /**
     * Create a DiscardStrategy for a {@link NDRobotBrain}'s player.
     *
     * @param ga   Our game
     * @param pl   Our player data in {@code ga}
     * @param br   Robot brain for {@code pl}
     * @param rand Random number generator from {@code br}
     */
    public NDDiscardStrategy(SOCGame ga, SOCPlayer pl, NDRobotBrain br, Random rand) {
        super(ga, pl, br, rand);
    }

    @Override
    public SOCResourceSet discard(int numDiscards, Stack<SOCPossiblePiece> buildingPlan) {
        switch (DecisionTreeType.whichUse(this.game, this.ourPlayerData)) {
            case LONGEST_ROAD:
                return discardPrioritize(numDiscards, Arrays.asList(ROAD, SETTLEMENT));
            case LARGEST_ARMY:
                return discardPrioritize(numDiscards, Collections.singletonList(CARD));
            default:
                return discardPrioritize(numDiscards, Collections.emptyList());
        }
    }

    protected SOCResourceSet discardPrioritize(int numDiscards, List<Integer> typesToPrioritize) {
        SOCResourceSet discards = new SOCResourceSet(this.ourPlayerData.getResources());

        List<Integer> fullConsiderations = new ArrayList<>(typesToPrioritize);
        fullConsiderations.addAll(Arrays.asList(ROAD, SETTLEMENT, CARD, CITY));

        // try to keep resources that we can build with
        for(Integer type : fullConsiderations) {
            while(discards.getTotal() > numDiscards && NDHelpers.haveResourcesFor(type, brain, discards)) {
                SOCResourceSet resources = NDHelpers.getResourcesFor(type);
                if(discards.getTotal() - resources.getTotal() >= numDiscards) {
                    discards.subtract(resources);
                } else {
                    break;
                }
            }
        }

        //choose the rest based on relative rarity
        Map<Integer, Integer> probabilityForResource = NDHelpers.getProbabilityForResource(brain);
        return IntStream.range(SOCResourceConstants.MIN, SOCResourceConstants.MAXPLUSONE - 1)
                .filter(discards::contains)
                .boxed()
                //TODO consider changing the ranking for resources - consider what we need to build the priorities?
                .sorted(Comparator.comparing(probabilityForResource::get).reversed())
                .mapToInt(Integer::intValue)
                .flatMap(type -> IntStream.generate(() -> type).limit(discards.getAmount(type)))
                .limit(numDiscards)
                .collect(SOCResourceSet::new, (socResourceSet, type) -> socResourceSet.add(1, type), SOCResourceSet::add);
    }
}