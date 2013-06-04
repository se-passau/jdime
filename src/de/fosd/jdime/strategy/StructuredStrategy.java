/*******************************************************************************
 * Copyright (c) 2013 Olaf Lessenich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Olaf Lessenich - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package de.fosd.jdime.strategy;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.NotYetImplementedException;
import de.fosd.jdime.common.operations.MergeOperation;

/**
 * Performs a structured merge.
 * 
 * @author Olaf Lessenich
 * 
 */
public class StructuredStrategy extends MergeStrategy<ASTNodeArtifact> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fosd.jdime.strategy.MergeStrategy#merge(
	 * de.fosd.jdime.common.operations.MergeOperation, 
	 * de.fosd.jdime.common.MergeContext)
	 */
	@Override
	public final void merge(final MergeOperation<ASTNodeArtifact> operation,
			final MergeContext<ASTNodeArtifact> context) {
		// TODO Auto-generated method stub

		throw new NotYetImplementedException(
				"StructuredStrategy: Implement me!");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fosd.jdime.strategy.MergeStrategy#toString()
	 */
	@Override
	public final String toString() {
		return "structured";
	}

}
