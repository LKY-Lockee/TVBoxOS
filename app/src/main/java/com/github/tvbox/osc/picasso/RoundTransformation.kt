package com.github.tvbox.osc.picasso

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.IntDef
import androidx.core.graphics.createBitmap
import com.squareup.picasso.Transformation

/**
 * @author pj567
 * @since 2020/12/22
 */
class RoundTransformation(private val key: String?) : Transformation {
	private var viewWidth = 0
	private var viewHeight = 0
	private var bottomShapeHeight = 0

	@RoundType
	private var mRoundType = RoundType.NONE
	private var diameter = 0
	private var radius = 0
	private var isCenterCorp = true // 垂直方向不是中间裁剪，就是顶部

	fun override(width: Int, height: Int): RoundTransformation {
		this.viewWidth = width
		this.viewHeight = height
		return this
	}

	fun centerCorp(centerCorp: Boolean): RoundTransformation {
		this.isCenterCorp = centerCorp
		return this
	}

	fun bottomShapeHeight(shapeHeight: Int): RoundTransformation {
		this.bottomShapeHeight = shapeHeight
		return this
	}

	fun roundRadius(radius: Int, @RoundType mRoundType: Int): RoundTransformation {
		this.radius = radius
		this.diameter = radius * 2
		this.mRoundType = mRoundType
		return this
	}

	override fun transform(source: Bitmap): Bitmap {
		val sourceWidth = source.width
		val sourceHeight = source.height
		if (viewWidth == 0 || viewHeight == 0) {
			viewWidth = sourceWidth
			viewHeight = sourceHeight
		}
		val scale: Float
		val targetWidth: Int
		val targetHeight: Int
		if (sourceWidth != viewWidth || sourceHeight != viewHeight) {
			if (sourceWidth * 1f / viewWidth > sourceHeight * 1f / viewHeight) {
				scale = viewHeight.toFloat() / sourceHeight
				targetWidth = (sourceWidth * scale).toInt()
				targetHeight = viewHeight
			} else {
				scale = viewWidth.toFloat() / sourceWidth
				targetWidth = viewWidth
				targetHeight = (sourceHeight * scale).toInt()
			}
		} else {
			scale = 1f
			targetWidth = sourceWidth
			targetHeight = sourceHeight
		}
		val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
		if (scale != 1f) {
			val matrix = Matrix()
			matrix.setScale(scale, scale)
			shader.setLocalMatrix(matrix)
		}
		val bitmap = createBitmap(targetWidth, targetHeight)
		bitmap.setHasAlpha(true)
		val paint = Paint(Paint.ANTI_ALIAS_FLAG)
		paint.shader = shader
		val canvas = Canvas(bitmap)
		val rect = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
		canvas.drawRoundRect(rect, radius.toFloat(), radius.toFloat(), paint)

		source.recycle()
		return bitmap
	}

	private fun drawBottomLabel(mCanvas: Canvas, mPaint: Paint, left: Float, top: Float, right: Float, bottom: Float) {
		if (bottomShapeHeight <= 0) return
		mPaint.shader = null
		mPaint.color = -0x67000000
		mCanvas.drawPath(roundedRect(left, bottom - bottomShapeHeight * 2, right, bottom, radius.toFloat(), radius.toFloat(), tl = false, tr = false, br = true, bl = true), mPaint)
	}

	private fun drawRoundRect(mCanvas: Canvas, mPaint: Paint, width: Float, height: Float) {
		when (mRoundType) {
			RoundType.NONE -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRect(RectF(0f, 0f, width, height), mPaint)
			} else {
				if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
					val dis = (height - viewHeight) / 2f
					if (isCenterCorp) {
						mCanvas.translate(0f, -dis)
						mCanvas.drawRect(RectF(0f, dis, viewWidth.toFloat(), viewHeight + dis), mPaint)
					} else {
						mCanvas.drawRect(RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
					}
				} else {
					val dis = (width - viewWidth) / 2f
					mCanvas.translate(-dis, 0f)
					mCanvas.drawRect(RectF(dis, 0f, viewWidth + dis, viewHeight.toFloat()), mPaint)
				}
			}

			RoundType.ALL -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRoundRect(RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				drawBottomLabel(mCanvas, mPaint, 0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
			} else if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
				val dis = (height - viewHeight) / 2f
				if (isCenterCorp) {
					mCanvas.translate(0f, -dis)
					mCanvas.drawRoundRect(RectF(0f, dis, viewWidth.toFloat(), viewHeight + dis), radius.toFloat(), radius.toFloat(), mPaint)
					drawBottomLabel(mCanvas, mPaint, 0f, dis, viewWidth.toFloat(), viewHeight + dis)
				} else {
					mCanvas.drawRoundRect(RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
					drawBottomLabel(mCanvas, mPaint, 0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
				}
			} else {
				val dis = (width - viewWidth) / 2f
				mCanvas.translate(-dis, 0f)
				mCanvas.drawRoundRect(RectF(dis, 0f, viewWidth + dis, viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				drawBottomLabel(mCanvas, mPaint, dis, 0f, viewWidth + dis, viewHeight.toFloat())
			}

			RoundType.TOP -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRoundRect(RectF(0f, 0f, viewWidth.toFloat(), diameter.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(0f, radius.toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
			} else if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
				val dis = (height - viewHeight) / 2f
				if (isCenterCorp) {
					mCanvas.translate(0f, -dis)
					mCanvas.drawRoundRect(RectF(0f, dis, viewWidth.toFloat(), diameter + dis), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, dis + radius, viewWidth.toFloat(), viewHeight + dis), mPaint)
				} else {
					mCanvas.drawRoundRect(RectF(0f, 0f, viewWidth.toFloat(), diameter.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, radius.toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
				}
			} else {
				val dis = (width - viewWidth) / 2f
				mCanvas.translate(-dis, 0f)
				mCanvas.drawRoundRect(RectF(dis, 0f, viewWidth + dis, diameter.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(dis, radius.toFloat(), viewWidth + dis, viewHeight.toFloat()), mPaint)
			}

			RoundType.RIGHT -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRoundRect(RectF((viewWidth - diameter).toFloat(), 0f, viewWidth.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(0f, 0f, (viewWidth - radius).toFloat(), viewHeight.toFloat()), mPaint)
			} else if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
				val dis = (height - viewHeight) / 2f
				if (isCenterCorp) {
					mCanvas.translate(0f, -dis)
					mCanvas.drawRoundRect(RectF((viewWidth - diameter).toFloat(), dis, viewWidth.toFloat(), viewHeight + dis), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, dis, (viewWidth - radius).toFloat(), viewHeight + dis), mPaint)
				} else {
					mCanvas.drawRoundRect(RectF((viewWidth - diameter).toFloat(), 0f, viewWidth.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, 0f, (viewWidth - radius).toFloat(), viewHeight.toFloat()), mPaint)
				}
			} else {
				val dis = (width - viewWidth) / 2f
				mCanvas.translate(-dis, 0f)
				mCanvas.drawRoundRect(RectF(viewWidth - diameter + dis, 0f, viewWidth + dis, viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(dis, 0f, viewWidth - radius + dis, viewHeight.toFloat()), mPaint)
			}

			RoundType.BOTTOM -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRoundRect(RectF(0f, (viewHeight - diameter).toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(0f, 0f, viewWidth.toFloat(), (viewHeight - radius).toFloat()), mPaint)
			} else if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
				val dis = (height - viewHeight) / 2f
				if (isCenterCorp) {
					mCanvas.translate(0f, -dis)
					mCanvas.drawRoundRect(RectF(0f, viewHeight - diameter + dis, viewWidth.toFloat(), viewHeight + dis), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, dis, viewWidth.toFloat(), viewHeight - radius + dis), mPaint)
				} else {
					mCanvas.drawRoundRect(RectF(0f, (viewHeight - diameter).toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, 0f, viewWidth.toFloat(), (viewHeight - radius).toFloat()), mPaint)
				}
			} else {
				val dis = (width - viewWidth) / 2f
				mCanvas.translate(-dis, 0f)
				mCanvas.drawRoundRect(RectF(dis, (viewHeight - diameter).toFloat(), viewWidth + dis, viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(dis, 0f, viewWidth + dis, (viewHeight - radius).toFloat()), mPaint)
			}

			RoundType.LEFT -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRoundRect(RectF(0f, 0f, diameter.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(radius.toFloat(), 0f, viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
			} else if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
				val dis = (height - viewHeight) / 2f
				if (isCenterCorp) {
					mCanvas.translate(0f, -dis)
					mCanvas.drawRoundRect(RectF(0f, dis, diameter.toFloat(), viewHeight + dis), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(radius.toFloat(), dis, viewWidth.toFloat(), viewHeight + dis), mPaint)
				} else {
					mCanvas.drawRoundRect(RectF(0f, 0f, diameter.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(radius.toFloat(), 0f, viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
				}
			} else {
				val dis = (width - viewWidth) / 2f
				mCanvas.translate(-dis, 0f)
				mCanvas.drawRoundRect(RectF(dis, 0f, diameter + dis, viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(radius + dis, 0f, viewWidth + dis, viewHeight.toFloat()), mPaint)
			}

			RoundType.LEFT_TOP -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRoundRect(RectF(0f, 0f, diameter.toFloat(), diameter.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(radius.toFloat(), 0f, viewWidth.toFloat(), radius.toFloat()), mPaint)
				mCanvas.drawRect(RectF(0f, radius.toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
			} else if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
				val dis = (height - viewHeight) / 2f
				if (isCenterCorp) {
					mCanvas.translate(0f, -dis)
					mCanvas.drawRoundRect(RectF(0f, dis, diameter.toFloat(), diameter + dis), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(radius.toFloat(), dis, viewWidth.toFloat(), radius + dis), mPaint)
					mCanvas.drawRect(RectF(0f, radius + dis, viewWidth.toFloat(), viewHeight + dis), mPaint)
				} else {
					mCanvas.drawRoundRect(RectF(0f, 0f, diameter.toFloat(), diameter.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(radius.toFloat(), 0f, viewWidth.toFloat(), radius.toFloat()), mPaint)
					mCanvas.drawRect(RectF(0f, radius.toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
				}
			} else {
				val dis = (width - viewWidth) / 2f
				mCanvas.translate(-dis, 0f)
				mCanvas.drawRoundRect(RectF(dis, 0f, diameter + dis, diameter.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(radius + dis, 0f, viewWidth + dis, radius.toFloat()), mPaint)
				mCanvas.drawRect(RectF(dis, radius.toFloat(), viewWidth + dis, viewHeight.toFloat()), mPaint)
			}

			RoundType.LEFT_BOTTOM -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRoundRect(RectF(0f, (viewHeight - diameter).toFloat(), diameter.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(0f, 0f, viewWidth.toFloat(), (viewHeight - radius).toFloat()), mPaint)
				mCanvas.drawRect(RectF(radius.toFloat(), (viewHeight - radius).toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
			} else if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
				val dis = (height - viewHeight) / 2f
				if (isCenterCorp) {
					mCanvas.translate(0f, -dis)
					mCanvas.drawRoundRect(RectF(0f, viewHeight - diameter + dis, diameter.toFloat(), viewHeight + dis), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, dis, viewWidth.toFloat(), viewHeight - radius + dis), mPaint)
					mCanvas.drawRect(RectF(radius.toFloat(), viewHeight - radius + dis, viewWidth.toFloat(), viewHeight + dis), mPaint)
				} else {
					mCanvas.drawRoundRect(RectF(0f, (viewHeight - diameter).toFloat(), diameter.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, 0f, viewWidth.toFloat(), (viewHeight - radius).toFloat()), mPaint)
					mCanvas.drawRect(RectF(radius.toFloat(), (viewHeight - radius).toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
				}
			} else {
				val dis = (width - viewWidth) / 2f
				mCanvas.translate(-dis, 0f)
				mCanvas.drawRoundRect(RectF(dis, (viewHeight - diameter).toFloat(), diameter + dis, viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(dis, 0f, viewWidth + dis, (viewHeight - radius).toFloat()), mPaint)
				mCanvas.drawRect(RectF(radius + dis, (viewHeight - radius).toFloat(), viewWidth + dis, viewHeight.toFloat()), mPaint)
			}

			RoundType.RIGHT_TOP -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRoundRect(RectF((viewWidth - diameter).toFloat(), 0f, viewWidth.toFloat(), diameter.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(0f, 0f, (viewWidth - radius).toFloat(), radius.toFloat()), mPaint)
				mCanvas.drawRect(RectF(0f, radius.toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
			} else if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
				val dis = (height - viewHeight) / 2f
				if (isCenterCorp) {
					mCanvas.translate(0f, -dis)
					mCanvas.drawRoundRect(RectF((viewWidth - diameter).toFloat(), dis, viewWidth.toFloat(), diameter + dis), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, dis, (viewWidth - radius).toFloat(), radius + dis), mPaint)
					mCanvas.drawRect(RectF(0f, radius + dis, viewWidth.toFloat(), viewHeight + dis), mPaint)
				} else {
					mCanvas.drawRoundRect(RectF((viewWidth - diameter).toFloat(), 0f, viewWidth.toFloat(), diameter.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, 0f, (viewWidth - radius).toFloat(), radius.toFloat()), mPaint)
					mCanvas.drawRect(RectF(0f, radius.toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), mPaint)
				}
			} else {
				val dis = (width - viewWidth) / 2f
				mCanvas.translate(-dis, 0f)
				mCanvas.drawRoundRect(RectF(viewWidth - diameter + dis, 0f, viewWidth + dis, diameter.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(dis, 0f, viewWidth - radius + dis, radius.toFloat()), mPaint)
				mCanvas.drawRect(RectF(dis, radius.toFloat(), viewWidth + dis, viewHeight.toFloat()), mPaint)
			}

			RoundType.RIGHT_BOTTOM -> if (viewWidth.toFloat() == width && viewHeight.toFloat() == height) {
				mCanvas.drawRoundRect(RectF((viewWidth - diameter).toFloat(), (viewHeight - diameter).toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(0f, 0f, viewWidth.toFloat(), (viewHeight - radius).toFloat()), mPaint)
				mCanvas.drawRect(RectF(0f, (viewHeight - radius).toFloat(), (viewWidth - radius).toFloat(), viewHeight.toFloat()), mPaint)
			} else if (viewWidth.toFloat() == width && viewHeight.toFloat() != height) {
				val dis = (height - viewHeight) / 2f
				if (isCenterCorp) {
					mCanvas.translate(0f, -dis)
					mCanvas.drawRoundRect(RectF((viewWidth - diameter).toFloat(), viewHeight - diameter + dis, viewWidth.toFloat(), viewHeight + dis), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, dis, viewWidth.toFloat(), viewHeight - radius + dis), mPaint)
					mCanvas.drawRect(RectF(0f, viewHeight - radius + dis, (viewWidth - radius).toFloat(), viewHeight + dis), mPaint)
				} else {
					mCanvas.drawRoundRect(RectF((viewWidth - diameter).toFloat(), (viewHeight - diameter).toFloat(), viewWidth.toFloat(), viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
					mCanvas.drawRect(RectF(0f, 0f, viewWidth.toFloat(), (viewHeight - radius).toFloat()), mPaint)
					mCanvas.drawRect(RectF(0f, (viewHeight - radius).toFloat(), (viewWidth - radius).toFloat(), viewHeight.toFloat()), mPaint)
				}
			} else {
				val dis = (width - viewWidth) / 2f
				mCanvas.translate(-dis, 0f)
				mCanvas.drawRoundRect(RectF(viewWidth - diameter + dis, (viewHeight - diameter).toFloat(), viewWidth + dis, viewHeight.toFloat()), radius.toFloat(), radius.toFloat(), mPaint)
				mCanvas.drawRect(RectF(dis, 0f, viewWidth + dis, (viewHeight - radius).toFloat()), mPaint)
				mCanvas.drawRect(RectF(dis, (viewHeight - radius).toFloat(), viewWidth - radius + dis, viewHeight.toFloat()), mPaint)
			}
		}
	}

	override fun key(): String? {
		return key
	}

	@IntDef(RoundType.ALL, RoundType.TOP, RoundType.RIGHT, RoundType.BOTTOM, RoundType.LEFT, RoundType.LEFT_TOP, RoundType.LEFT_BOTTOM, RoundType.RIGHT_TOP, RoundType.RIGHT_BOTTOM, RoundType.NONE)
	@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER)
	@Retention(AnnotationRetention.SOURCE)
	annotation class RoundType {
		companion object {
			const val ALL: Int = 0
			const val TOP: Int = 1
			const val RIGHT: Int = 2
			const val BOTTOM: Int = 3
			const val LEFT: Int = 4
			const val LEFT_TOP: Int = 5
			const val LEFT_BOTTOM: Int = 6
			const val RIGHT_TOP: Int = 7
			const val RIGHT_BOTTOM: Int = 8
			const val NONE: Int = 9
		}
	}

	companion object {
		fun roundedRect(left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, tl: Boolean, tr: Boolean, br: Boolean, bl: Boolean): Path {
			var rx = rx
			var ry = ry
			val path = Path()
			if (rx < 0) rx = 0f
			if (ry < 0) ry = 0f
			val width = right - left
			val height = bottom - top
			if (rx > width / 2) rx = width / 2
			if (ry > height / 2) ry = height / 2
			val widthMinusCorners = (width - (2 * rx))
			val heightMinusCorners = (height - (2 * ry))

			path.moveTo(right, top + ry)
			if (tr) path.rQuadTo(0f, -ry, -rx, -ry) // top-right corner
			else {
				path.rLineTo(0f, -ry)
				path.rLineTo(-rx, 0f)
			}
			path.rLineTo(-widthMinusCorners, 0f)
			if (tl) path.rQuadTo(-rx, 0f, -rx, ry) // top-left corner
			else {
				path.rLineTo(-rx, 0f)
				path.rLineTo(0f, ry)
			}
			path.rLineTo(0f, heightMinusCorners)

			if (bl) path.rQuadTo(0f, ry, rx, ry) // bottom-left corner
			else {
				path.rLineTo(0f, ry)
				path.rLineTo(rx, 0f)
			}

			path.rLineTo(widthMinusCorners, 0f)
			if (br) path.rQuadTo(rx, 0f, rx, -ry) // bottom-right corner
			else {
				path.rLineTo(rx, 0f)
				path.rLineTo(0f, -ry)
			}

			path.rLineTo(0f, -heightMinusCorners)

			path.close() // Given close, last lineTo can be removed.

			return path
		}
	}
}
