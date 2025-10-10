/**
 * Core FaceBytes API and result types.
 *
 * <p>This package contains the main DeepFace-compatible API surface, including
 * verification, search (find), face representation (embeddings), face extraction,
 * and facial analysis entry points. Records in this package model outputs such as
 * verification results, embeddings, face regions, and analysis results.</p>
 *
 * <p>Design goals:</p>
 * <ul>
 *   <li>Keep method names compatible with DeepFace: verify, find, analyze, represent, extractFaces</li>
 *   <li>Stateless, functional-style methods for easy use in Spring apps</li>
 *   <li>Security-first: validate inputs, size limits, and structured logging</li>
 * </ul>
 */
package io.github.codesapienbe.springvision.facebytes.core;
