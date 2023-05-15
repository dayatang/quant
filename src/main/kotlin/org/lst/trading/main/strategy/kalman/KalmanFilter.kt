package org.lst.trading.main.strategy.kalman

import org.la4j.LinearAlgebra
import org.la4j.Matrix
import org.la4j.matrix.DenseMatrix

/**
 * n: Number of states
 * m: Number of sensors
 *
 *
 * https://www.udacity.com/wiki/cs373/kalman-filter-matrices
 *
 *
 * Steps:
 *
 *
 * PREDICT X'
 *
 *
 * First, we predict our next x:
 *
 *
 * x' = Fx + u
 *
 *
 * UPDATE P'
 *
 *
 * We also update the covariance matrix according to the next prediction:
 *
 *
 * P' = FP(transp F)
 *
 *
 * UPDATE Y
 *
 *
 * y becomes the difference between the move and what we expected:
 *
 *
 * y = z - Hx
 *
 *
 * UPDATE S
 *
 *
 * S is the covariance of the move, adding up the covariance in move space of the position and the covariance of the measurement:
 *
 *
 * S = HP(transp H) + R
 *
 *
 * CALCULATE K
 *
 *
 * Now I start to wave my hands. I assume this next matrix is called K because this is the work of the Kalman filter. Whatever is happening here, it doesn't depend on u or z. We're computing how much of the difference between the observed move and the expected move to add to x.
 *
 *
 * K = P (transp H) (inv S)
 *
 *
 * UPDATE X'
 *
 *
 * We update x:
 *
 *
 * x' = x + Ky
 *
 *
 * SUBTRACT P
 *
 *
 * And we subtract some uncertainty from P, again not depending on u or z:
 *
 *
 * P' = P - P(transp H)(inv S)HP
 */
class KalmanFilter(// n
    private val stateCount: Int, // m
    private val sensorCount: Int
) {

    /**
     * stateCount x 1
     *
     *
     * The control input, the move vector.
     * It's the change to x that we cause, or that we know is happening.
     * Since we add it to x, it has dimension n. When the filter updates, it adds u to the new x.
     *
     *
     * External moves to the system.
     */
    private var moveVector: Matrix = Matrix.zero(stateCount, 1)

    // state
    /**
     * stateCount x 1
     */
    var state: Matrix? = null // x, state estimate

    /**
     * stateCount x stateCount
     *
     *
     * Symmetric.
     * Down the diagonal of P, we find the variances of the elements of x.
     * On the off diagonals, at P[i][j], we find the covariances of x[i] with x[j].
     */
    var stateCovariance: Matrix? = null // Covariance matrix of x, process noise (w)
    // predict
    /**
     * stateCount x stateCount
     *
     *
     * Kalman filters model a system over time.
     * After each tick of time, we predict what the values of x are, and then we measure and do some computation.
     * F is used in the update step. Here's how it works: For each value in x, we write an equation to update that value,
     * a linear equation in all the variables in x. Then we can just read off the coefficients to make the matrix.
     */
    var updateMatrix: Matrix? = null // F, State transition matrix.

    /**
     * stateCount x stateCount
     *
     *
     * Error in the process, after each update this uncertainty is added.
     */
    var updateCovariance: Matrix? = null // Q, Estimated error in process.
   // measurement
    /**
     * sensorCount x 1
     *
     *
     * z: Measurement Vector, It's the outputs from the sensors.
     */
    private var mMeasurement: Matrix? = null

    /**
     * sensorCount x sensorCount
     *
     *
     * R, the variances and covariances of our sensor measurements.
     *
     *
     * The Kalman filter algorithm does not change R, because the process can't change our belief about the
     * accuracy of our sensors--that's a property of the sensors themselves.
     * We know the variance of our sensor either by testing it, or by reading the documentation that came with it,
     * or something like that. Note that the covariances here are the covariances of the measurement error.
     * A positive number means that if the first sensor is erroneously low, the second tends to be erroneously low,
     * or if the first reads high, the second tends to read high; it doesn't mean that if the first sensor reports a
     * high number the second will also report a high number
     */
    var measurementCovariance: Matrix? = null // R, Covariance matrix of the measurement vector z

    /**
     * sensorCount x stateCount
     *
     *
     * The matrix H tells us what sensor readings we'd get if x were the true state of affairs and our sensors were perfect.
     * It's the matrix we use to extract the measurement from the data.
     * If we multiply H times a perfectly correct x, we get a perfectly correct z.
     */
    var extractionMatrix: Matrix? = null // H, Observation matrix.

    // no inputs
    var innovation: Matrix? = null
        private set
    var innovationCovariance: Matrix? = null
        private set

    private fun step() {
        // prediction
        val predictedState = updateMatrix!!.multiply(state).add(moveVector)
        val predictedStateCovariance =
            updateMatrix!!.multiply(stateCovariance).multiply(updateMatrix!!.transpose()).add(
                updateCovariance
            )

        // observation
        innovation = mMeasurement!!.subtract(extractionMatrix!!.multiply(predictedState))
        innovationCovariance =
            extractionMatrix!!.multiply(predictedStateCovariance).multiply(extractionMatrix!!.transpose()).add(
                measurementCovariance
            )

        // update
        val kalmanGain = predictedStateCovariance.multiply(extractionMatrix!!.transpose()).multiply(
            innovationCovariance!!.withInverter(LinearAlgebra.InverterFactory.SMART).inverse()
        )
        state = predictedState.add(kalmanGain.multiply(innovation))
        val nRow = stateCovariance!!.rows()
        stateCovariance = DenseMatrix.identity(nRow).subtract(kalmanGain.multiply(extractionMatrix))
            .multiply(predictedStateCovariance)
    }

    fun step(measurement: Matrix?, move: Matrix) {
        mMeasurement = measurement
        moveVector = move
        step()
    }

    fun step(measurement: Matrix?) {
        mMeasurement = measurement
        step()
    }
}
