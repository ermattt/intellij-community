// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.ml.impl.approach

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.platform.ml.*
import com.intellij.platform.ml.ScopeEnvironment.Companion.narrowedTo
import com.intellij.platform.ml.impl.*
import com.intellij.platform.ml.impl.MLTaskApproach.Companion.startMLSession
import com.intellij.platform.ml.impl.apiPlatform.MLApiPlatform
import com.intellij.platform.ml.impl.apiPlatform.MLApiPlatform.Companion.getDescriptorsOfTiers
import com.intellij.platform.ml.impl.apiPlatform.MLApiPlatform.Companion.getJoinedListenerForTask
import com.intellij.platform.ml.impl.model.MLModel
import com.intellij.platform.ml.impl.monitoring.MLApproachListener
import com.intellij.platform.ml.impl.monitoring.MLSessionListener
import com.intellij.platform.ml.impl.session.*
import org.jetbrains.annotations.ApiStatus

/**
 * The main way to apply classical machine learning approaches: run the ML model, collect the logs, retrain the model, repeat.
 *
 * @param task The task that is solved by this approach.
 * @param apiPlatform The platform, that the approach will be running within.
 */
@ApiStatus.Internal
abstract class LogDrivenModelInference<M : MLModel<P>, P : Any>(
  override val task: MLTask<P>,
  override val apiPlatform: MLApiPlatform
) : MLTaskApproach<P> {}

/**
 * An exception that indicates that for some reason, it was not possible to provide an ML model
 * when calling [com.intellij.platform.ml.impl.model.MLModel.Provider.provideModel].
 * The session's start is considered as failed.
 */
@ApiStatus.Internal
class ModelNotAcquiredOutcome<P : Any> : Session.StartOutcome.Failure<P>() {
  override val failureDetails: String = "ML Model was not provided"
}
