package io.codera.quant.util

import com.google.common.base.Preconditions
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix
import java.util.*

/**
 * Utility methods used across math package.
 */
object MathUtil {
    fun trimr(arr: DoubleArray, n1: Int, n2: Int): DoubleArray {
        Preconditions.checkArgument(n1 + n2 < arr.size, "Attempting to trim too much in trimr")
        val h2 = arr.size - n2
        return arr.copyOfRange(n1, h2)
    }

    fun trimr(arr: Array<DoubleArray>, n1: Int, n2: Int): Array<DoubleArray> {
        Preconditions.checkArgument(n1 + n2 < arr.size, "Attempting to trim too much in trimr")
        val h2 = arr.size - n2
        return arr.copyOfRange(n1, h2)
    }

    fun trimr(m: RealMatrix, n1: Int, n2: Int): RealMatrix {
        Preconditions.checkArgument(n1 + n2 < m.rowDimension, "Attempting to trim too much in trimr")
        val h2 = m.rowDimension - 1 - n2
        return m.getSubMatrix(n1, h2, 0, m.columnDimension - 1)
    }

    fun tdiff(arr: DoubleArray, k: Int): DoubleArray {
        if (k == 0) {
            return arr
        }
        val newArr = DoubleArray(arr.size)
        for (i in 0 until arr.size - k) {
            newArr[i + k] = arr[i + k] - arr[i]
        }
        return newArr
    }

    fun tdiff(m: RealMatrix, k: Int): RealMatrix {
        if (k == 0) {
            return m
        }
        val rSize = m.rowDimension
        val cSize = m.columnDimension
        val newMatrix: RealMatrix = Array2DRowRealMatrix(rSize, cSize)
        for (i in rSize - 1 downTo k) {
            for (j in 0 until cSize) {
                newMatrix.setEntry(i, j, m.getEntry(i, j) - m.getEntry(i - k, j))
            }
        }
        for (i in 0 until k) {
            for (j in 0 until cSize) {
                newMatrix.setEntry(i, j, 0.0)
            }
        }
        return newMatrix
    }

    fun pTrend(p: Int, nobs: Int): Array<DoubleArray> {
        val arr = Array(nobs) { DoubleArray(p + 1) }
        for (i in 0 until nobs) {
            arr[i][0] = 1.0
        }
        if (p > 0) {
            for (i in 0 until nobs) {
                for (j in 1 until p + 1) {
                    if (j == 1) {
                        arr[i][j] = (i + 1).toDouble() / nobs
                    } else {
                        arr[i][j] = Math.pow(arr[i][1], j.toDouble())
                    }
                }
            }
        }
        return arr
    }

    /**
     * Creates a matrix or vector of lagged values.
     *
     * @param arr input matrix or vector, (nobs x k)
     * @param n order of lag
     * @param v (optional) initial values (default=0)
     * @return z = matrix (or vector) of lags (nobs x k)
     */
    fun lag(arr: DoubleArray, n: Int, v: Int): DoubleArray {
        if (n == 0) {
            return arr
        }
        val newArr = DoubleArray(arr.size)
        for (i in arr.indices.reversed()) {
            if (i < n) {
                newArr[i] = v.toDouble()
            } else {
                newArr[i] = arr[i - n]
            }
        }
        return newArr
    }

    fun lag(arr: Array<DoubleArray>, n: Int, v: Int): Array<DoubleArray> {
        if (n == 0) {
            return arr
        }
        for (i in arr.indices.reversed()) {
            for (j in arr[i].indices) {
                if (i < n) {
                    arr[i][j] = v.toDouble()
                } else {
                    arr[i][j] = arr[i - n][j]
                }
            }
        }
        return arr
    }

    fun lag(m: RealMatrix, n: Int, v: Int): RealMatrix {
        if (n == 0) {
            return m
        }
        val newMatrix: RealMatrix = Array2DRowRealMatrix(m.rowDimension, m.columnDimension)
        for (i in m.rowDimension - 1 downTo 0) {
            for (j in 0 until m.columnDimension) {
                if (i < n) {
                    newMatrix.setEntry(i, j, v.toDouble())
                } else {
                    newMatrix.setEntry(i, j, m.getEntry(i - n, j))
                }
            }
        }
        return newMatrix
    }

    fun prependArrayAsColumn(arr: DoubleArray, twoDArr: Array<DoubleArray>): Array<DoubleArray> {
        Preconditions.checkArgument(arr.size == twoDArr.size, "array sizes are not equal")
        for (i in twoDArr.indices) {
            val newRow = DoubleArray(twoDArr[i].size + 1)
            newRow[0] = arr[i]
            System.arraycopy(twoDArr[i], 0, newRow, 1, twoDArr[i].size)
            twoDArr[i] = newRow
        }
        return twoDArr
    }

    fun prependArrayAsColumn(arr: DoubleArray, arr2: DoubleArray): Array<DoubleArray?> {
        Preconditions.checkArgument(arr.size == arr2.size, "array sizes are not equal")
        val newArr = arrayOfNulls<DoubleArray>(arr.size)
        for (i in arr.indices) {
            newArr[i] = doubleArrayOf(arr[i], arr2[i])
        }
        return newArr
    }

    fun zCrit(nobs: Int, p: Int): DoubleArray {
        Preconditions.checkArgument(nobs >= 50, "nobs can not be less than 50")
        val zt = arrayOf(
            doubleArrayOf(-2.63467, -1.95254, -1.62044, 0.910216, 1.30508, 2.08088),
            doubleArrayOf(-3.63993, -2.94935, -2.61560, -0.369306 - 0.0116304, 0.666745),
            doubleArrayOf(-4.20045, -3.54490, -3.21450, -1.20773, -0.896215, -0.237604),
            doubleArrayOf(-4.65813, -3.99463, -3.66223, -1.69214, -1.39031, -0.819931),
            doubleArrayOf(-5.07175, -4.39197, -4.03090, -2.06503, -1.78329, -1.21830),
            doubleArrayOf(-5.45384, -4.73277, -4.39304, -2.40333, -2.15433, -1.62357),
            doubleArrayOf(-5.82090, -5.13053, -4.73415, -2.66466, -2.39868, -1.88193),
            doubleArrayOf(-2.53279, -1.94976, -1.62656, 0.915249, 1.31679, 2.11787),
            doubleArrayOf(-3.56634, -2.93701, -2.61518, -0.439283 - 0.0498821, 0.694244),
            doubleArrayOf(-4.08920, -3.46145, -3.17093, -1.25839, -0.919533, -0.298641),
            doubleArrayOf(-4.56873, -3.89966, -3.59161, -1.72543, -1.44513, -0.894085),
            doubleArrayOf(-4.97062, -4.33552, -4.00795, -2.12519, -1.85785, -1.30566),
            doubleArrayOf(-5.26901, -4.62509, -4.29928, -2.42113, -2.15002, -1.65832),
            doubleArrayOf(-5.54856, -4.95553, -4.63476, -2.71763, -2.46508, -1.99450),
            doubleArrayOf(-2.60249, -1.94232, -1.59497, 0.912961, 1.30709, 2.02375),
            doubleArrayOf(-3.43911, -2.91515, -2.58414, -0.404598 - 0.0481033, 0.538450),
            doubleArrayOf(-4.00519, -3.46110, -3.15517, -1.25332, -0.958071, -0.320677),
            doubleArrayOf(-4.46919, -3.87624, -3.58887, -1.70354, -1.44034, -0.920625),
            doubleArrayOf(-4.84725, -4.25239, -3.95439, -2.11382, -1.85495, -1.26406),
            doubleArrayOf(-5.15555, -4.59557, -4.30149, -2.41271, -2.19370, -1.70447),
            doubleArrayOf(-5.46544, -4.89343, -4.58188, -2.74151, -2.49723, -2.02390),
            doubleArrayOf(-2.58559, -1.94477, -1.62458, 0.905676, 1.30371, 2.01881),
            doubleArrayOf(-3.46419, -2.91242, -2.58837, -0.410558 - 0.0141618, 0.665034),
            doubleArrayOf(-4.00090, -3.45423, -3.16252, -1.24040, -0.937658, -0.304433),
            doubleArrayOf(-4.45303, -3.89216, -3.61209, -1.74246, -1.48280, -0.906047),
            doubleArrayOf(-4.79484, -4.22115, -3.92941, -2.11434, -1.83632, -1.30274),
            doubleArrayOf(-5.15005, -4.58359, -4.30336, -2.44972, -2.21312, -1.68330),
            doubleArrayOf(-5.42757, -4.88604, -4.60358, -2.74044, -2.50205, -2.04008),
            doubleArrayOf(-2.65229, -1.99090, -1.66577, 0.875165, 1.27068, 2.04414),
            doubleArrayOf(-3.49260, -2.87595, -2.56885, -0.416310 - 0.0488941, 0.611200),
            doubleArrayOf(-3.99417, -3.42290, -3.13981, -1.25096, -0.950916, -0.310521),
            doubleArrayOf(-4.42462, -3.85645, -3.56568, -1.73108, -1.45873, -0.934604),
            doubleArrayOf(-4.72243, -4.22262, -3.94435, -2.10660, -1.84233, -1.26702),
            doubleArrayOf(-5.12654, -4.55072, -4.24765, -2.43456, -2.18887, -1.73081),
            doubleArrayOf(-5.46995, -4.87930, -4.57608, -2.71226, -2.48367, -2.00597),
            doubleArrayOf(-2.63492, -1.96775, -1.62969, 0.904516, 1.31371, 2.03286),
            doubleArrayOf(-3.44558, -2.84182, -2.57313, -0.469204, -0.128358, 0.553411),
            doubleArrayOf(-3.99140, -3.41543, -3.13588, -1.23585, -0.944500, -0.311271),
            doubleArrayOf(-4.43404, -3.84922, -3.56413, -1.73854, -1.48585, -0.896978),
            doubleArrayOf(-4.75946, -4.19562, -3.91052, -2.09997, -1.86034, -1.32987),
            doubleArrayOf(-5.14042, -4.56772, -4.25699, -2.43882, -2.18922, -1.67371),
            doubleArrayOf(-5.39389, -4.85343, -4.57927, -2.73497, -2.49921, -2.00247),
            doubleArrayOf(-2.58970, -1.95674, -1.61786, 0.902516, 1.32215, 2.05383),
            doubleArrayOf(-3.44036, -2.86974, -2.58294, -0.451590 - 0.0789340, 0.631864),
            doubleArrayOf(-3.95420, -3.43052, -3.13924, -1.23328, -0.938986, -0.375491),
            doubleArrayOf(-4.40180, -3.79982, -3.52726, -1.71598, -1.44584, -0.885303),
            doubleArrayOf(-4.77897, -4.21672, -3.93324, -2.12309, -1.88431, -1.33916),
            doubleArrayOf(-5.13508, -4.56464, -4.27617, -2.44358, -2.18826, -1.72784),
            doubleArrayOf(-5.35071, -4.82097, -4.54914, -2.73377, -2.48874, -2.01437),
            doubleArrayOf(-2.60653, -1.96391, -1.63477, 0.890881, 1.29296, 1.97163),
            doubleArrayOf(-3.42692, -2.86280, -2.57220, -0.463397 - 0.0922419, 0.613101),
            doubleArrayOf(-3.99299, -3.41999, -3.13524, -1.23857, -0.929915, -0.337193),
            doubleArrayOf(-4.41297, -3.83582, -3.55450, -1.72408, -1.44915, -0.872755),
            doubleArrayOf(-4.75811, -4.18759, -3.92599, -2.12799, -1.88463, -1.37118),
            doubleArrayOf(-5.08726, -4.53617, -4.26643, -2.44694, -2.19109, -1.72329),
            doubleArrayOf(-5.33780, -4.82542, -4.54802, -2.73460, -2.50726, -2.02927),
            doubleArrayOf(-2.58687, -1.93939, -1.63192, 0.871242, 1.26611, 1.96641),
            doubleArrayOf(-3.38577, -2.86443, -2.57318, -0.391939 - 0.0498984, 0.659539),
            doubleArrayOf(-3.93785, -3.39130, -3.10317, -1.24836, -0.956349, -0.334478),
            doubleArrayOf(-4.39967, -3.85724, -3.55951, -1.74578, -1.46374, -0.870275),
            doubleArrayOf(-4.74764, -4.20488, -3.91350, -2.12384, -1.88202, -1.36853),
            doubleArrayOf(-5.07739, -4.52487, -4.25185, -2.43674, -2.22289, -1.72955),
            doubleArrayOf(-5.36172, -4.81947, -4.53837, -2.74448, -2.51367, -2.03065),
            doubleArrayOf(-2.58364, -1.95730, -1.63110, 0.903082, 1.28613, 2.00605),
            doubleArrayOf(-3.45830, -2.87104, -2.59369, -0.451613, -0.106025, 0.536687),
            doubleArrayOf(-3.99783, -3.43182, -3.16171, -1.26032, -0.956327, -0.305719),
            doubleArrayOf(-4.40298, -3.86066, -3.56940, -1.74588, -1.48429, -0.914111),
            doubleArrayOf(-4.84459, -4.23012, -3.93845, -2.15135, -1.89876, -1.39654),
            doubleArrayOf(-5.10571, -4.56846, -4.28913, -2.47637, -2.22517, -1.79586),
            doubleArrayOf(-5.39872, -4.86396, -4.58525, -2.78971, -2.56181, -2.14042)
        )
        var i = Math.round(nobs.toDouble() / 50).toInt() + 1
        if (nobs < 50) {
            i--
        }
        if (i > 10) {
            i = 10
        }
        i = (i - 1) * 7 + p + 2
        return zt[i - 1]
    }

    fun rztcrit(nobs: Int, k: Int, p: Int): DoubleArray {
        var zt: Array<DoubleArray>? = null
        if (nobs >= 500) {
            zt = arrayOf(
                doubleArrayOf(-3.28608, -2.71123, -2.44427, -0.228267, 0.196845, 1.07845),
                doubleArrayOf(-3.88031, -3.35851, -3.03798, -1.01144, -0.653342, 0.153117),
                doubleArrayOf(-4.36339, -3.84931, -3.52926, -1.59069, -1.27691, -0.688550),
                doubleArrayOf(-4.69226, -4.16473, -3.91069, -2.03499, -1.75167, -1.16909),
                doubleArrayOf(-5.12583, -4.55603, -4.24350, -2.43062, -2.15918, -1.63241),
                doubleArrayOf(-5.45902, -4.85433, -4.54552, -2.68999, -2.45059, -1.96213),
                doubleArrayOf(-5.68874, -5.13084, -4.85451, -3.01287, -2.77470, -2.34774),
                doubleArrayOf(-3.95399, -3.33181, -3.01057, -0.964258, -0.632140, 0.148153),
                doubleArrayOf(-4.29147, -3.77581, -3.47606, -1.47435, -1.15649, -0.382089),
                doubleArrayOf(-4.80216, -4.16163, -3.87422, -1.95661, -1.68975, -1.17624),
                doubleArrayOf(-5.08973, -4.49148, -4.22534, -2.34763, -2.09506, -1.52368),
                doubleArrayOf(-5.28946, -4.77944, -4.49057, -2.63483, -2.39227, -1.88262),
                doubleArrayOf(-5.64107, -5.10086, -4.81771, -2.95313, -2.74233, -2.30293),
                doubleArrayOf(-5.84555, -5.26853, -5.01340, -3.21419, -2.95790, -2.50159),
                doubleArrayOf(-4.25439, -3.69759, -3.42840, -1.49852, -1.22694, -0.593763),
                doubleArrayOf(-4.62332, -4.12603, -3.83833, -1.91632, -1.65271, -0.937750),
                doubleArrayOf(-5.09990, -4.50073, -4.18896, -2.26553, -1.97459, -1.41616),
                doubleArrayOf(-5.23982, -4.74879, -4.50065, -2.59004, -2.30601, -1.76624),
                doubleArrayOf(-5.63745, -5.07700, -4.77794, -2.88029, -2.66305, -2.25529),
                doubleArrayOf(-5.87733, -5.31763, -5.03729, -3.17526, -2.94043, -2.54329),
                doubleArrayOf(-6.08463, -5.57014, -5.29279, -3.45890, -3.21035, -2.68331),
                doubleArrayOf(-4.68825, -4.14264, -3.83668, -1.89022, -1.62543, -1.02171),
                doubleArrayOf(-5.00664, -4.43544, -4.14709, -2.24334, -1.94304, -1.29258),
                doubleArrayOf(-5.42102, -4.77343, -4.48998, -2.57209, -2.30366, -1.79885),
                doubleArrayOf(-5.60249, -5.02686, -4.77574, -2.89195, -2.61726, -2.09253),
                doubleArrayOf(-5.90744, -5.31272, -5.04121, -3.16076, -2.89667, -2.44274),
                doubleArrayOf(-6.16639, -5.58218, -5.28049, -3.40263, -3.15765, -2.70251),
                doubleArrayOf(-6.29638, -5.79252, -5.52324, -3.65372, -3.40115, -2.94514),
                doubleArrayOf(-4.99327, -4.43088, -4.13314, -2.19577, -1.94806, -1.33955),
                doubleArrayOf(-5.28724, -4.72773, -4.46224, -2.52556, -2.25121, -1.75592),
                doubleArrayOf(-5.53603, -5.03231, -4.74442, -2.81101, -2.53978, -2.01464),
                doubleArrayOf(-5.85790, -5.28516, -4.99765, -3.11650, -2.85684, -2.38643),
                doubleArrayOf(-6.03218, -5.50167, -5.24244, -3.37898, -3.13182, -2.57977),
                doubleArrayOf(-6.38137, -5.80056, -5.52693, -3.62856, -3.37482, -2.85511),
                doubleArrayOf(-6.60394, -6.03056, -5.73651, -3.83174, -3.56048, -3.09560)
            )
        } else if (nobs >= 400 && nobs <= 499) {
            zt = arrayOf(
                doubleArrayOf(-3.39320, -2.78062, -2.47410, -0.279165, 0.172570, 1.01757),
                doubleArrayOf(-3.81898, -3.34274, -3.04197, -0.984635, -0.632195, 0.0786160),
                doubleArrayOf(-4.43824, -3.83476, -3.53856, -1.59769, -1.32538, -0.682733),
                doubleArrayOf(-4.78731, -4.19879, -3.90468, -2.03620, -1.78519, -1.25540),
                doubleArrayOf(-5.15859, -4.55815, -4.27559, -2.40402, -2.15148, -1.64991),
                doubleArrayOf(-5.36666, -4.82211, -4.55480, -2.73039, -2.47586, -1.96342),
                doubleArrayOf(-5.70533, -5.14149, -4.83768, -2.98968, -2.75467, -2.33244),
                doubleArrayOf(-3.88099, -3.31554, -3.00918, -1.01400, -0.666507, 0.112207),
                doubleArrayOf(-4.35920, -3.76677, -3.47891, -1.47887, -1.17461, -0.457611),
                doubleArrayOf(-4.73655, -4.17175, -3.87843, -1.95622, -1.67273, -1.05752),
                doubleArrayOf(-5.03407, -4.48465, -4.18736, -2.32047, -2.06844, -1.54620),
                doubleArrayOf(-5.37301, -4.80609, -4.50790, -2.65816, -2.39100, -1.90516),
                doubleArrayOf(-5.63842, -5.08273, -4.79419, -2.95211, -2.72047, -2.26114),
                doubleArrayOf(-5.95823, -5.38482, -5.08735, -3.23862, -2.98661, -2.58060),
                doubleArrayOf(-4.29209, -3.74752, -3.44785, -1.49664, -1.19363, -0.540536),
                doubleArrayOf(-4.73620, -4.16373, -3.83159, -1.87826, -1.56786, -0.906299),
                doubleArrayOf(-4.98331, -4.47817, -4.18238, -2.27544, -1.99733, -1.45956),
                doubleArrayOf(-5.34322, -4.77455, -4.47877, -2.60581, -2.34669, -1.82075),
                doubleArrayOf(-5.61331, -5.05800, -4.77543, -2.91228, -2.64829, -2.13015),
                doubleArrayOf(-5.94606, -5.34094, -5.05669, -3.17314, -2.92833, -2.50131),
                doubleArrayOf(-6.17994, -5.62560, -5.32022, -3.45919, -3.21928, -2.73838),
                doubleArrayOf(-4.68326, -4.13893, -3.83504, -1.88594, -1.59783, -1.02900),
                doubleArrayOf(-5.01959, -4.44111, -4.16075, -2.24225, -1.96550, -1.36753),
                doubleArrayOf(-5.35312, -4.76318, -4.48253, -2.53350, -2.26862, -1.74966),
                doubleArrayOf(-5.65846, -5.05443, -4.74318, -2.86021, -2.61633, -2.15096),
                doubleArrayOf(-5.89297, -5.33097, -5.03686, -3.13780, -2.88399, -2.36895),
                doubleArrayOf(-6.11791, -5.59035, -5.29834, -3.39283, -3.13194, -2.64558),
                doubleArrayOf(-6.43463, -5.83831, -5.54375, -3.63526, -3.40822, -2.97731),
                doubleArrayOf(-4.99049, -4.45174, -4.15603, -2.22388, -1.94107, -1.40933),
                doubleArrayOf(-5.37057, -4.77929, -4.48921, -2.54431, -2.27297, -1.72675),
                doubleArrayOf(-5.61805, -5.06136, -4.76461, -2.81651, -2.54785, -2.04956),
                doubleArrayOf(-5.88425, -5.29788, -5.01558, -3.10698, -2.83781, -2.33035),
                doubleArrayOf(-6.15156, -5.57259, -5.28198, -3.36062, -3.10140, -2.61065),
                doubleArrayOf(-6.37314, -5.80031, -5.51577, -3.63686, -3.38505, -2.87176),
                doubleArrayOf(-6.58251, -6.03057, -5.74573, -3.85037, -3.60485, -3.11932)
            )
        } else if (nobs >= 300 && nobs <= 399) {
            zt = arrayOf(
                doubleArrayOf(-3.36203, -2.77548, -2.46139, -0.286807, 0.132866, 1.03471),
                doubleArrayOf(-3.90239, -3.32711, -3.03723, -0.996528, -0.605509, 0.118508),
                doubleArrayOf(-4.32982, -3.81156, -3.51879, -1.59453, -1.29025, -0.576746),
                doubleArrayOf(-4.81264, -4.24058, -3.93314, -2.05226, -1.79734, -1.23867),
                doubleArrayOf(-5.09929, -4.53317, -4.26022, -2.39047, -2.15062, -1.66121),
                doubleArrayOf(-5.40020, -4.84728, -4.56541, -2.72073, -2.48276, -2.01238),
                doubleArrayOf(-5.72554, -5.14543, -4.85290, -3.03642, -2.79747, -2.38877),
                doubleArrayOf(-3.93064, -3.31039, -3.00695, -1.02551, -0.692057, 0.104883),
                doubleArrayOf(-4.30844, -3.76971, -3.48291, -1.49867, -1.18293, -0.449296),
                doubleArrayOf(-4.69802, -4.16002, -3.85937, -1.95172, -1.66941, -1.07873),
                doubleArrayOf(-5.09621, -4.51913, -4.22178, -2.32005, -2.06940, -1.52440),
                doubleArrayOf(-5.39988, -4.84499, -4.54918, -2.66241, -2.40886, -1.94518),
                doubleArrayOf(-5.67194, -5.12143, -4.83266, -2.95787, -2.71575, -2.26783),
                doubleArrayOf(-5.90971, -5.38093, -5.10006, -3.24590, -3.00999, -2.55590),
                doubleArrayOf(-4.32518, -3.77645, -3.46220, -1.48724, -1.19931, -0.531819),
                doubleArrayOf(-4.66166, -4.12423, -3.82665, -1.85992, -1.56770, -0.952556),
                doubleArrayOf(-5.06263, -4.47715, -4.19478, -2.27228, -1.98935, -1.40857),
                doubleArrayOf(-5.39577, -4.79037, -4.51644, -2.60186, -2.32067, -1.82448),
                doubleArrayOf(-5.62591, -5.09997, -4.78451, -2.89543, -2.66108, -2.16281),
                doubleArrayOf(-5.96117, -5.38487, -5.08529, -3.19176, -2.95677, -2.45750),
                doubleArrayOf(-6.18044, -5.61962, -5.32402, -3.44453, -3.18600, -2.75024),
                doubleArrayOf(-4.69949, -4.11581, -3.84809, -1.91652, -1.63097, -1.06354),
                doubleArrayOf(-5.02878, -4.48050, -4.18169, -2.20023, -1.92196, -1.37122),
                doubleArrayOf(-5.37891, -4.82102, -4.49501, -2.55100, -2.29407, -1.76313),
                doubleArrayOf(-5.59926, -5.07560, -4.78056, -2.89047, -2.61834, -2.11372),
                doubleArrayOf(-5.97404, -5.35040, -5.03148, -3.15838, -2.91666, -2.44570),
                doubleArrayOf(-6.20250, -5.64756, -5.33112, -3.40255, -3.16800, -2.73795),
                doubleArrayOf(-6.40258, -5.84695, -5.58164, -3.67811, -3.42766, -2.97315),
                doubleArrayOf(-5.02873, -4.44103, -4.15164, -2.19792, -1.94100, -1.39467),
                doubleArrayOf(-5.36834, -4.76996, -4.46992, -2.53666, -2.27257, -1.73355),
                doubleArrayOf(-5.59537, -5.05016, -4.78520, -2.83093, -2.57279, -2.07503),
                doubleArrayOf(-5.85590, -5.33224, -5.03207, -3.11489, -2.86007, -2.36551),
                doubleArrayOf(-6.20771, -5.62475, -5.32273, -3.36439, -3.10806, -2.63899),
                doubleArrayOf(-6.38397, -5.87287, -5.56819, -3.63376, -3.37917, -2.87215),
                doubleArrayOf(-6.69353, -6.08474, -5.78590, -3.87231, -3.61022, -3.14908)
            )
        } else if (nobs >= 200 && nobs <= 299) {
            zt = arrayOf(
                doubleArrayOf(-3.35671, -2.77519, -2.46594, -0.254099, 0.196134, 1.07222),
                doubleArrayOf(-3.92428, -3.38037, -3.08215, -1.00759, -0.634217, 0.0945623),
                doubleArrayOf(-4.48168, -3.83395, -3.54540, -1.60205, -1.31840, -0.734322),
                doubleArrayOf(-4.82954, -4.23468, -3.94803, -2.05472, -1.80434, -1.27245),
                doubleArrayOf(-5.19748, -4.57984, -4.28594, -2.42219, -2.18483, -1.73071),
                doubleArrayOf(-5.48348, -4.89872, -4.60436, -2.75423, -2.51959, -2.06231),
                doubleArrayOf(-5.82241, -5.21284, -4.90675, -3.03145, -2.79112, -2.38818),
                doubleArrayOf(-3.88242, -3.33232, -3.01999, -0.988265, -0.633419, 0.121320),
                doubleArrayOf(-4.36630, -3.76414, -3.46091, -1.48625, -1.15077, -0.498422),
                doubleArrayOf(-4.76842, -4.20038, -3.89975, -1.93433, -1.63407, -1.04290),
                doubleArrayOf(-5.05007, -4.54203, -4.23534, -2.35721, -2.10330, -1.57965),
                doubleArrayOf(-5.46384, -4.89647, -4.60567, -2.66674, -2.41227, -1.92884),
                doubleArrayOf(-5.80068, -5.17731, -4.86360, -2.97354, -2.71548, -2.25152),
                doubleArrayOf(-6.01552, -5.48792, -5.18651, -3.27732, -3.05193, -2.62313),
                doubleArrayOf(-4.37038, -3.77348, -3.48123, -1.46468, -1.19712, -0.522913),
                doubleArrayOf(-4.71164, -4.17296, -3.87214, -1.88824, -1.61792, -0.998973),
                doubleArrayOf(-5.07287, -4.49791, -4.19539, -2.25537, -1.97775, -1.42073),
                doubleArrayOf(-5.43158, -4.85660, -4.55542, -2.59513, -2.34448, -1.88253),
                doubleArrayOf(-5.71928, -5.15509, -4.85008, -2.91869, -2.67892, -2.16537),
                doubleArrayOf(-5.95901, -5.38920, -5.10190, -3.21921, -2.97088, -2.49105),
                doubleArrayOf(-6.24842, -5.69150, -5.39236, -3.47876, -3.22814, -2.81954),
                doubleArrayOf(-4.76132, -4.12120, -3.81887, -1.87640, -1.57988, -0.959247),
                doubleArrayOf(-5.07595, -4.49599, -4.18062, -2.22181, -1.95429, -1.32816),
                doubleArrayOf(-5.41865, -4.82420, -4.51442, -2.54584, -2.28898, -1.71129),
                doubleArrayOf(-5.69988, -5.10837, -4.81872, -2.87861, -2.62537, -2.10745),
                doubleArrayOf(-6.03815, -5.41121, -5.11067, -3.15726, -2.89572, -2.39236),
                doubleArrayOf(-6.31746, -5.67322, -5.35729, -3.42445, -3.18255, -2.72287),
                doubleArrayOf(-6.54722, -5.92036, -5.63475, -3.68619, -3.44087, -2.99590),
                doubleArrayOf(-5.06954, -4.48980, -4.16461, -2.22770, -1.95682, -1.39685),
                doubleArrayOf(-5.35737, -4.81634, -4.52940, -2.54416, -2.26355, -1.73669),
                doubleArrayOf(-5.65024, -5.06222, -4.78444, -2.84019, -2.55801, -2.03438),
                doubleArrayOf(-6.01717, -5.38593, -5.07183, -3.10854, -2.83015, -2.38316),
                doubleArrayOf(-6.22810, -5.62644, -5.32983, -3.37920, -3.11022, -2.58412),
                doubleArrayOf(-6.51923, -5.91250, -5.61917, -3.64604, -3.37807, -2.91979),
                doubleArrayOf(-6.74433, -6.15641, -5.85483, -3.88559, -3.62884, -3.22791)
            )
        } else if (nobs >= 1 && nobs <= 199) {
            zt = arrayOf(
                doubleArrayOf(-3.40026, -2.81980, -2.49012, -0.284064, 0.162780, 0.991182),
                doubleArrayOf(-4.02456, -3.40397, -3.08903, -0.998765, -0.638257, 0.0929366),
                doubleArrayOf(-4.50406, -3.91574, -3.60618, -1.64640, -1.34126, -0.674994),
                doubleArrayOf(-4.97750, -4.31424, -4.00116, -2.07039, -1.80758, -1.24622),
                doubleArrayOf(-5.29795, -4.65255, -4.36236, -2.43756, -2.20744, -1.74384),
                doubleArrayOf(-5.69006, -5.02821, -4.70153, -2.78533, -2.55054, -2.12221),
                doubleArrayOf(-6.01114, -5.32900, -5.01614, -3.10458, -2.87108, -2.45944),
                doubleArrayOf(-4.03875, -3.38465, -3.06445, -1.01452, -0.670171, 0.0830536),
                doubleArrayOf(-4.49697, -3.83781, -3.52924, -1.50657, -1.18131, -0.494574),
                doubleArrayOf(-4.85358, -4.24290, -3.92668, -1.93268, -1.67668, -1.11969),
                doubleArrayOf(-5.23415, -4.63779, -4.32076, -2.35203, -2.10299, -1.58236),
                doubleArrayOf(-5.60428, -4.99996, -4.67591, -2.71512, -2.45663, -1.97999),
                doubleArrayOf(-5.89816, -5.30839, -4.98307, -3.01998, -2.78403, -2.33971),
                doubleArrayOf(-6.24667, -5.61312, -5.28841, -3.32373, -3.07681, -2.65243),
                doubleArrayOf(-4.50725, -3.84730, -3.53859, -1.50198, -1.21063, -0.494936),
                doubleArrayOf(-4.87844, -4.22489, -3.92431, -1.88702, -1.59187, -0.972172),
                doubleArrayOf(-5.20113, -4.56724, -4.27167, -2.29534, -2.03226, -1.43479),
                doubleArrayOf(-5.61984, -4.95138, -4.63381, -2.62062, -2.34903, -1.81713),
                doubleArrayOf(-5.93516, -5.26326, -4.95702, -2.97158, -2.70668, -2.22094),
                doubleArrayOf(-6.20848, -5.57967, -5.28403, -3.27115, -3.01521, -2.58367),
                doubleArrayOf(-6.52806, -5.84919, -5.55596, -3.54144, -3.30790, -2.88872),
                doubleArrayOf(-4.84291, -4.21809, -3.89360, -1.88296, -1.62337, -0.998749),
                doubleArrayOf(-5.18976, -4.56495, -4.23781, -2.23973, -1.95745, -1.36282),
                doubleArrayOf(-5.49570, -4.91049, -4.57949, -2.54844, -2.30040, -1.81108),
                doubleArrayOf(-5.85200, -5.24753, -4.90738, -2.89515, -2.62635, -2.11513),
                doubleArrayOf(-6.25788, -5.59734, -5.23154, -3.20543, -2.95304, -2.49876),
                doubleArrayOf(-6.42744, -5.80415, -5.49459, -3.46836, -3.20457, -2.78454),
                doubleArrayOf(-6.79276, -6.11558, -5.77461, -3.74987, -3.49703, -3.07378),
                doubleArrayOf(-5.25985, -4.56675, -4.25742, -2.24159, -1.93760, -1.40055),
                doubleArrayOf(-5.53963, -4.88523, -4.55008, -2.53159, -2.26558, -1.74469),
                doubleArrayOf(-5.86277, -5.23537, -4.92559, -2.84160, -2.58154, -2.08171),
                doubleArrayOf(-6.16676, -5.52360, -5.22425, -3.12455, -2.84785, -2.41246),
                doubleArrayOf(-6.43205, -5.80308, -5.46594, -3.42417, -3.19918, -2.69791),
                doubleArrayOf(-6.81177, -6.11377, -5.74083, -3.67826, -3.41996, -2.95145),
                doubleArrayOf(-6.98960, -6.36882, -6.03754, -3.95573, -3.71192, -3.30766)
            )
        }
        if (k < 1 || k > 5) {
            return DoubleArray(6)
        }
        if (p > 5) {
            return DoubleArray(6)
        }
        if (zt == null) {
            return DoubleArray(6)
        }
        val n = (k - 1) * 7 + p + 1
        return zt[n]
    }

    fun detrend(timeSeries: DoubleArray, p: Int): Array<DoubleArray> {
        if (p == -1) {
            return MatrixUtils.createColumnRealMatrix(timeSeries).data
        }
        val u = ones(timeSeries.size)
        return if (p > 0) {
            throw UnsupportedOperationException("only p 0 and -1 supported")
        } else {
            val xMat = MatrixUtils.createColumnRealMatrix(u)
            val yMat = MatrixUtils.createColumnRealMatrix(timeSeries)
            val xpxi = MatrixUtils.inverse(xMat.transpose().multiply(xMat))
            val beta = xpxi.multiply(xMat.transpose().multiply(yMat))
            yMat.subtract(xMat.multiply(beta)).data
        }
    }

    fun detrend(m: RealMatrix, p: Int): RealMatrix {
        if (p == -1) {
            return m
        }
        val u = ones(m.rowDimension)
        return if (p > 0) {
            throw UnsupportedOperationException("only p 0 and -1 supported")
        } else {
            val xMat = MatrixUtils.createColumnRealMatrix(u)
            val xpxi = MatrixUtils.inverse(xMat.transpose().multiply(xMat))
            val beta = xpxi.multiply(xMat.transpose().multiply(m))
            m.subtract(xMat.multiply(beta))
        }
    }

    fun ones(arrSize: Int): DoubleArray {
        val onesArray = DoubleArray(arrSize)
        Arrays.fill(onesArray, 1.0)
        return onesArray
    }
}
