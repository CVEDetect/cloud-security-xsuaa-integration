/**
 * SPDX-FileCopyrightText: 2018-2022 SAP SE or an SAP affiliate company and Cloud Security Client Java contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sap.cloud.security.token;

import java.util.Collection;
import java.util.Set;

public interface ScopeConverter {
	Set<String> convert(Collection<String> scopes);
}
