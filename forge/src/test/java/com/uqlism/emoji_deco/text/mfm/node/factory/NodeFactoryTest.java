package com.uqlism.emoji_deco.text.mfm.node.factory;

import com.uqlism.emoji_deco.text.mfm.node.IMfmNode;
import com.uqlism.emoji_deco.text.mfm.node.IMfmNodeChildrenHolder;
import com.uqlism.emoji_deco.text.mfm.node.MfmBold;
import com.uqlism.emoji_deco.text.mfm.node.MfmText;
import com.uqlism.emoji_deco.text.mfm.node.factory.internal.INodeFactoryContext;
import com.uqlism.emoji_deco.text.mfm.parser.core.FoundType;
import com.uqlism.emoji_deco.text.mfm.parser.core.SubstringFoundInfo;
import com.uqlism.emoji_deco.text.mfm.parser.core.utils.IntRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link NodeFactory}, in particular its nest-level guard — a DoS mitigation against
 * adversarially-nested chat input that must always short-circuit to a verbatim {@link MfmText}
 * once {@code nestLevel > maximumNestLevel} (see {@link NodeFactory} class javadoc).
 *
 * <p>Since {@code parser/inline} and {@code parser/block} are ported in a later stage, these tests
 * build {@link SubstringFoundInfo} trees by hand instead of running the real MFM parser — this is
 * exactly the shape {@code BoldAstaParser} et al. would produce for {@code **...**} input.
 */
class NodeFactoryTest {

    /** A parse result and the input string it describes, built by {@link #nestedBold}. */
    private record Built(String text, SubstringFoundInfo info) {
    }

    /** One level of {@code **...**} wrapping either a leaf {@code FoundType.Text} or another level. */
    private static Built nestedBold(int depth, String leafText) {
        if (depth == 0) {
            IntRange range = new IntRange(0, leafText.length());
            return new Built(leafText, new SubstringFoundInfo(FoundType.Text, range, range, leafText.length()));
        }

        Built inner = nestedBold(depth - 1, leafText);
        String text = "**" + inner.text() + "**";
        IntRange overallRange = new IntRange(0, text.length());
        IntRange contentRange = new IntRange(2, 2 + inner.text().length());
        SubstringFoundInfo shiftedInner = shift(inner.info(), 2);
        SubstringFoundInfo info = new SubstringFoundInfo(
                FoundType.BoldAsta, overallRange, contentRange, text.length(), List.of(shiftedInner));
        return new Built(text, info);
    }

    private static SubstringFoundInfo shift(SubstringFoundInfo info, int offset) {
        List<SubstringFoundInfo> nested = info.nestedInfos().stream()
                .map(n -> shift(n, offset))
                .collect(Collectors.toList());
        return new SubstringFoundInfo(
                info.type(),
                info.overallRange().offset(offset),
                info.contentRange().offset(offset),
                info.resumeIndex() + offset,
                nested);
    }

    @Test
    void createsBoldNodeWrappingTextFromHandBuiltFoundInfo() {
        Built built = nestedBold(1, "hello");
        assertEquals("**hello**", built.text());

        List<IMfmNode> nodes = NodeFactory.createNodes(built.text(), List.of(built.info()), new INodeFactoryContext.Impl());

        assertEquals(1, nodes.size());
        MfmBold bold = assertInstanceOf(MfmBold.class, nodes.get(0));
        assertEquals("**hello**", bold.stringify());
        assertEquals(List.of(new MfmText("hello")), bold.children());
    }

    @Test
    void roundTripsWhenNestingStaysAtOrBelowTheLimit() {
        // maximumNestLevel=2, depth=2: nestLevel reaches exactly 2 at the deepest level, which is
        // still allowed ("nestLevel > maximumNestLevel" is a strict inequality).
        Built built = nestedBold(2, "x");
        INodeFactoryContext context = new INodeFactoryContext.Impl(2, 0);

        List<IMfmNode> nodes = NodeFactory.createNodes(built.text(), List.of(built.info()), context);

        assertEquals(1, nodes.size());
        assertEquals(built.text(), nodes.get(0).stringify());
        assertInstanceOf(MfmBold.class, nodes.get(0));
        assertInstanceOf(MfmBold.class, ((IMfmNodeChildrenHolder) nodes.get(0)).children().get(0));
    }

    @Test
    void nestLevelGuardTruncatesToVerbatimTextOnceLimitIsExceeded() {
        // maximumNestLevel=2, depth=5: levels 1-2 parse normally into MfmBold, but level 3 (where
        // nestLevel becomes 3 > 2) must be cut off and emitted as a single literal MfmText — not
        // recursed into further, and not silently dropped.
        Built built = nestedBold(5, "x");
        INodeFactoryContext context = new INodeFactoryContext.Impl(2, 0);

        List<IMfmNode> nodes = NodeFactory.createNodes(built.text(), List.of(built.info()), context);

        assertEquals(1, nodes.size());
        MfmBold level1 = assertInstanceOf(MfmBold.class, nodes.get(0));
        MfmBold level2 = assertInstanceOf(MfmBold.class, level1.children().get(0));
        // level 3 must NOT be an MfmBold: the guard tripped and it was frozen into MfmText verbatim.
        MfmText level3 = assertInstanceOf(MfmText.class, level2.children().get(0));

        // The frozen text is the untouched slice of the original input for that subtree (still
        // containing unparsed "**" markers from levels 3-5), proving the guard used
        // foundInfo.overallRange() rather than dropping or re-parsing the content.
        assertTrue(level3.text().startsWith("**"));
        assertTrue(level3.text().endsWith("**"));
        assertTrue(level3.text().contains("x"));

        // The context must be restored to its starting nest level after the call returns.
        assertEquals(0, context.nestLevel());
    }

    @Test
    void nestLevelGuardTripsImmediatelyWhenContextStartsAtTheLimit() {
        // maximumNestLevel=0: even a single, otherwise-valid BoldAsta must be frozen verbatim,
        // since deepenNest() increments nestLevel to 1 before the guard check runs (1 > 0).
        Built built = nestedBold(1, "hello");
        INodeFactoryContext context = new INodeFactoryContext.Impl(0, 0);

        List<IMfmNode> nodes = NodeFactory.createNodes(built.text(), List.of(built.info()), context);

        assertEquals(1, nodes.size());
        MfmText text = assertInstanceOf(MfmText.class, nodes.get(0));
        assertEquals(built.text(), text.text());
    }

    @Test
    void substringMirrorsKotlinIntRangeSubstringSemantics() {
        assertEquals("ell", NodeFactory.substring("hello", new IntRange(1, 4)));
    }
}
