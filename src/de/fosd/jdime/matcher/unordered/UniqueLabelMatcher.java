/**
 * 
 */
package de.fosd.jdime.matcher.unordered;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.Matching;

/**
 * @author Olaf Lessenich
 * 
 * @param <T>
 *            type of artifact
 */
public class UniqueLabelMatcher<T extends Artifact<T>> extends
		UnorderedMatcher<T> {

	/**
	 * @param matcher
	 *            matcher
	 */
	public UniqueLabelMatcher(final Matcher<T> matcher) {
		super(matcher);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.matcher.unordered.UnorderedMatcher#match(de.fosd.jdime.
	 * common.Artifact, de.fosd.jdime.common.Artifact)
	 */
	@Override
	public final Matching<T> match(final T left, final T right) {
		if (!left.matches(right)) {
			return new Matching<T>(left, right, 0);
		}

		if (left.getNumChildren() == 0 || right.getNumChildren() == 0) {
			return new Matching<T>(left, right, 1);
		}

		List<Matching<T>> childrenMatchings = new LinkedList<Matching<T>>();
		List<T> leftChildren = left.getChildren();
		List<T> rightChildren = right.getChildren();

		Collections.sort(leftChildren);
		Collections.sort(rightChildren);

		Iterator<T> leftIt = leftChildren.iterator();
		Iterator<T> rightIt = rightChildren.iterator();
		T leftChild = (T) leftIt.next();
		T rightChild = (T) rightIt.next();
		int sum = 0;

		boolean done = false;
		while (!done) {
			int c = leftChild.compareTo(rightChild);
			if (c < 0) {
				if (leftIt.hasNext()) {
					leftChild = leftIt.next();
				} else {
					done = true;
				}
			} else if (c > 0) {
				if (rightIt.hasNext()) {
					rightChild = rightIt.next();
				} else {
					done = true;
				}
			} else if (c == 0) {
				// matching
				Matching<T> childMatching = matcher
						.match(leftChild, rightChild);
				childrenMatchings.add(childMatching);
				sum += childMatching.getScore();
				if (leftIt.hasNext() && rightIt.hasNext()) {
					leftChild = leftIt.next();
					rightChild = rightIt.next();
				} else {
					done = true;
				}

			}
		}

		// do {
		// while (leftChild.compareTo(rightChild) < 0 && leftIt.hasNext()) {
		// leftChild = leftIt.next();
		// }
		//
		// while (leftChild.compareTo(rightChild) == 0
		// && (leftIt.hasNext() || rightIt.hasNext())) {
		// Matching<T> childMatching
		// = matcher.match(leftChild, rightChild);
		// childrenMatchings.add(childMatching);
		// sum += childMatching.getScore();
		//
		// if (leftIt.hasNext()) {
		// leftChild = leftIt.next();
		// }
		//
		// if (rightIt.hasNext()) {
		// rightChild = rightIt.next();
		// }
		// }
		//
		// while (leftChild.compareTo(rightChild) > 0 && rightIt.hasNext()) {
		// rightChild = rightIt.next();
		// }
		// } while (leftIt.hasNext() || rightIt.hasNext());

		Matching<T> rootmatching = new Matching<T>(left, right, sum + 1);
		rootmatching.setChildren(childrenMatchings);
		return rootmatching;
	}

}
