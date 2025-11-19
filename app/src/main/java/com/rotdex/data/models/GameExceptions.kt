package com.rotdex.data.models

/**
 * Exception thrown when user doesn't have enough energy for an action
 */
class InsufficientEnergyException(message: String) : Exception(message)

/**
 * Exception thrown when user doesn't have enough coins for an action
 */
class InsufficientCoinsException(message: String) : Exception(message)

/**
 * Exception thrown when user doesn't have enough gems for an action
 */
class InsufficientGemsException(message: String) : Exception(message)
