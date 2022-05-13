/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.kiwix.kiwixmobile.intro

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.database.DataSetObserver
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Interpolator
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import org.kiwix.kiwixmobile.R
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CustomPageIndicator @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyle: Int = 0
) : View(context, attrs, defStyle), OnPageChangeListener, View.OnAttachStateChangeListener {
  // configurable attributes
  private val dotDiameter: Int
  private val gap: Int
  private val animDuration: Long
  private val unselectedColour: Int
  private val selectedColour: Int

  // derived from attributes
  private val dotRadius: Float
  private val halfDotRadius: Float
  private val animHalfDuration: Long
  private var dotTopY = 0f
  private var dotCenterY = 0f
  private var dotBottomY = 0f

  // ViewPager
  private var viewPager: ViewPager? = null

  // state
  private var pageCount = 0
  private var currentPage = 0
  private var previousPage = 0
  private var selectedDotX = 0f
  private var selectedDotInPosition = false
  private var dotCenterX: FloatArray? = null
  private lateinit var joiningFractions: FloatArray
  private var retreatingJoinX1 = 0f
  private var retreatingJoinX2 = 0f
  private lateinit var dotRevealFractions: FloatArray
  private var attachedToWindow = false
  private var pageChanging = false

  // drawing
  private val unselectedPaint: Paint
  private val selectedPaint: Paint
  private val combinedUnselectedPath: Path
  private val unselectedDotPath: Path
  private val unselectedDotLeftPath: Path
  private val unselectedDotRightPath: Path
  private val rectF: RectF

  // animation
  private var moveAnimation: ValueAnimator? = null
  private val joiningAnimationSet: AnimatorSet? = null
  private var retreatAnimation: PendingRetreatAnimator? = null
  private lateinit var revealAnimations: Array<PendingRevealAnimator?>
  private val interpolator: Interpolator

  // working values for beziers
  private var endX1 = 0f
  private var endY1 = 0f
  private var endX2 = 0f
  private var endY2 = 0f
  private var controlX1 = 0f
  private var controlY1 = 0f
  private var controlX2 = 0f
  private var controlY2 = 0f
  fun setViewPager(viewPager: ViewPager) {
    this.viewPager = viewPager
    viewPager.addOnPageChangeListener(this)
    setPageCount(viewPager.adapter!!.count)
    viewPager.adapter!!.registerDataSetObserver(object : DataSetObserver() {
      override fun onChanged() {
        setPageCount(this@CustomPageIndicator.viewPager!!.adapter!!.count)
      }
    })
    setCurrentPageImmediate()
  }

  override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    if (attachedToWindow) {
      var fraction = positionOffset
      val currentPosition = if (pageChanging) previousPage else currentPage
      var leftDotPosition = position
      // when swiping from #2 to #1 ViewPager reports position as 1 and a descending offset
      // need to convert this into our left-dot-based 'coordinate space'
      if (currentPosition != position) {
        fraction = 1f - positionOffset

        // if user scrolls completely to next page then the position param updates to that
        // new page but we're not ready to switch our 'current' page yet so adjust for that
        if (fraction == 1f) {
          leftDotPosition = min(currentPosition, position)
        }
      }
      setJoiningFraction(leftDotPosition, fraction)
    }
  }

  override fun onPageSelected(position: Int) {
    if (attachedToWindow) {
      // this is the main event we're interested in!
      setSelectedPage(position)
    } else {
      // when not attached, don't animate the move, just store immediately
      setCurrentPageImmediate()
    }
  }

  override fun onPageScrollStateChanged(state: Int) {
    // nothing to do
  }

  private fun setPageCount(pages: Int) {
    pageCount = pages
    resetState()
    requestLayout()
  }

  private fun calculateDotPositions(width: Int, height: Int) {
    val left = paddingLeft
    val top = paddingTop
    val right = width - paddingRight
    val bottom = height - paddingBottom
    val requiredWidth = requiredWidth
    val startLeft = left + (right - left - requiredWidth) / 2 + dotRadius
    dotCenterX = FloatArray(pageCount)
    for (i in 0 until pageCount) {
      dotCenterX!![i] = startLeft + i * (dotDiameter + gap)
    }
    // todo just top aligning for now… should make this smarter
    dotTopY = top.toFloat()
    dotCenterY = top + dotRadius
    dotBottomY = (top + dotDiameter).toFloat()
    setCurrentPageImmediate()
  }

  private fun setCurrentPageImmediate() {
    currentPage = if (viewPager != null) {
      viewPager!!.currentItem
    } else {
      0
    }
    if (dotCenterX != null && dotCenterX!!.isNotEmpty() &&
      (moveAnimation == null || !moveAnimation!!.isStarted)
    ) {
      selectedDotX = dotCenterX!![currentPage]
    }
  }

  private fun resetState() {
    joiningFractions = FloatArray(if (pageCount == 0) 0 else pageCount - 1)
    Arrays.fill(joiningFractions, 0f)
    dotRevealFractions = FloatArray(pageCount)
    Arrays.fill(dotRevealFractions, 0f)
    retreatingJoinX1 = INVALID_FRACTION
    retreatingJoinX2 = INVALID_FRACTION
    selectedDotInPosition = true
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val desiredHeight = desiredHeight
    val height: Int = when (MeasureSpec.getMode(heightMeasureSpec)) {
      MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
      MeasureSpec.AT_MOST -> min(desiredHeight, MeasureSpec.getSize(heightMeasureSpec))
      else -> desiredHeight
    }
    val desiredWidth = desiredWidth
    val width: Int = when (MeasureSpec.getMode(widthMeasureSpec)) {
      MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
      MeasureSpec.AT_MOST -> min(desiredWidth, MeasureSpec.getSize(widthMeasureSpec))
      else -> desiredWidth
    }
    setMeasuredDimension(width, height)
    calculateDotPositions(width, height)
  }

  private val desiredHeight: Int
    get() = paddingTop + dotDiameter + paddingBottom
  private val requiredWidth: Int
    get() = pageCount * dotDiameter + (pageCount - 1) * gap
  private val desiredWidth: Int
    get() = paddingLeft + requiredWidth + paddingRight

  override fun onViewAttachedToWindow(view: View) {
    attachedToWindow = true
  }

  override fun onViewDetachedFromWindow(view: View) {
    attachedToWindow = false
  }

  override fun onDraw(canvas: Canvas) {
    if (viewPager == null || pageCount == 0) return
    drawUnselected(canvas)
    drawSelected(canvas)
  }

  private fun drawUnselected(canvas: Canvas) {
    combinedUnselectedPath.rewind()

    // draw any settled, revealing or joining dots
    for (page in 0 until pageCount) {
      val nextXIndex = if (page == pageCount - 1) page else page + 1
      val unselectedPath = getUnselectedPath(
        page,
        dotCenterX!![page],
        dotCenterX!![nextXIndex],
        if (page == pageCount - 1) INVALID_FRACTION else joiningFractions[page],
        dotRevealFractions[page]
      )
      unselectedPath.addPath(combinedUnselectedPath)
      combinedUnselectedPath.addPath(unselectedPath)
    }
    // draw any retreating joins
    if (retreatingJoinX1 != INVALID_FRACTION) {
      val retreatingJoinPath = retreatingJoinPath
      combinedUnselectedPath.addPath(retreatingJoinPath)
    }
    canvas.drawPath(combinedUnselectedPath, unselectedPaint)
  }

  private fun getUnselectedPath(
    page: Int,
    centerX: Float,
    nextCenterX: Float,
    joiningFraction: Float,
    dotRevealFraction: Float
  ): Path {
    unselectedDotPath.rewind()
    if ((joiningFraction == 0f || joiningFraction == INVALID_FRACTION) &&
      dotRevealFraction == 0f && !(page == currentPage && selectedDotInPosition)
    ) {

      // case #1 – At rest
      unselectedDotPath.addCircle(dotCenterX!![page], dotCenterY, dotRadius, Path.Direction.CW)
    }
    if (joiningFraction > 0f && joiningFraction <= 0.5f && retreatingJoinX1 == INVALID_FRACTION) {
      calculateDotRightPath(centerX, nextCenterX, joiningFraction)
    }
    if (joiningFraction > 0.5f && joiningFraction < 1f && retreatingJoinX1 == INVALID_FRACTION) {

      // case #3 – Joining neighbour, combined curved

      // adjust the fraction so that it goes from 0.3 -> 1 to produce a more realistic 'join'
      val adjustedFraction = (joiningFraction - 0.2f) * 1.25f

      // start in the bottom left
      unselectedDotPath.moveTo(centerX, dotBottomY)

      // semi-circle to the top left
      rectF[centerX - dotRadius, dotTopY, centerX + dotRadius] = dotBottomY
      unselectedDotPath.arcTo(rectF, 90f, 180f, true)

      // bezier to the middle top of the join
      endX1 = centerX + dotRadius + gap / 2
      endY1 = dotCenterY - adjustedFraction * dotRadius
      controlX1 = endX1 - adjustedFraction * dotRadius
      controlY1 = dotTopY
      controlX2 = endX1 - (1 - adjustedFraction) * dotRadius
      controlY2 = endY1
      unselectedDotPath.cubicTo(
        controlX1, controlY1,
        controlX2, controlY2,
        endX1, endY1
      )

      // bezier to the top right of the join
      endX2 = nextCenterX
      endY2 = dotTopY
      controlX1 = endX1 + (1 - adjustedFraction) * dotRadius
      controlY1 = endY1
      controlX2 = endX1 + adjustedFraction * dotRadius
      controlY2 = dotTopY
      unselectedDotPath.cubicTo(
        controlX1, controlY1,
        controlX2, controlY2,
        endX2, endY2
      )

      // semi-circle to the bottom right
      rectF[nextCenterX - dotRadius, dotTopY, nextCenterX + dotRadius] = dotBottomY
      unselectedDotPath.arcTo(rectF, 270f, 180f, true)

      // bezier to the middle bottom of the join
      // endX1 stays the same
      endY1 = dotCenterY + adjustedFraction * dotRadius
      controlX1 = endX1 + adjustedFraction * dotRadius
      controlY1 = dotBottomY
      controlX2 = endX1 + (1 - adjustedFraction) * dotRadius
      controlY2 = endY1
      unselectedDotPath.cubicTo(
        controlX1, controlY1,
        controlX2, controlY2,
        endX1, endY1
      )

      // bezier back to the start point in the bottom left
      endX2 = centerX
      endY2 = dotBottomY
      controlX1 = endX1 - (1 - adjustedFraction) * dotRadius
      controlY1 = endY1
      controlX2 = endX1 - adjustedFraction * dotRadius
      controlY2 = endY2
      unselectedDotPath.cubicTo(
        controlX1, controlY1,
        controlX2, controlY2,
        endX2, endY2
      )
    }
    if (joiningFraction == 1f && retreatingJoinX1 == INVALID_FRACTION) {

      // case #4 Joining neighbour, combined straight technically we could use case 3 for this
      // situation as well but assume that this is an optimization rather than faffing around
      // with beziers just to draw a rounded rect
      rectF[centerX - dotRadius, dotTopY, nextCenterX + dotRadius] = dotBottomY
      unselectedDotPath.addRoundRect(rectF, dotRadius, dotRadius, Path.Direction.CW)
    }

    // case #5 is handled by #getRetreatingJoinPath()
    // this is done separately so that we can have a single retreating path spanning
    // multiple dots and therefore animate it's movement smoothly
    if (dotRevealFraction > MINIMAL_REVEAL) {

      // case #6 – previously hidden dot revealing
      unselectedDotPath.addCircle(
        centerX, dotCenterY, dotRevealFraction * dotRadius,
        Path.Direction.CW
      )
    }
    return unselectedDotPath
  }

  private fun calculateDotRightPath(
    centerX: Float,
    nextCenterX: Float,
    joiningFraction: Float
  ) {
    // case #2 – Joining neighbour, still separate

    // start with the left dot
    unselectedDotLeftPath.rewind()

    // start at the bottom center
    unselectedDotLeftPath.moveTo(centerX, dotBottomY)

    // semi circle to the top center
    rectF[centerX - dotRadius, dotTopY, centerX + dotRadius] = dotBottomY
    unselectedDotLeftPath.arcTo(rectF, 90f, 180f, true)

    // cubic to the right middle
    endX1 = centerX + dotRadius + joiningFraction * gap
    endY1 = dotCenterY
    controlX1 = centerX + halfDotRadius
    controlY1 = dotTopY
    controlX2 = endX1
    controlY2 = endY1 - halfDotRadius
    unselectedDotLeftPath.cubicTo(
      controlX1, controlY1,
      controlX2, controlY2,
      endX1, endY1
    )

    // cubic back to the bottom center
    endX2 = centerX
    endY2 = dotBottomY
    controlX1 = endX1
    controlY1 = endY1 + halfDotRadius
    controlX2 = centerX + halfDotRadius
    controlY2 = dotBottomY
    unselectedDotLeftPath.cubicTo(
      controlX1, controlY1,
      controlX2, controlY2,
      endX2, endY2
    )
    unselectedDotPath.addPath(unselectedDotLeftPath)

    // now do the next dot to the right
    unselectedDotRightPath.rewind()

    // start at the bottom center
    unselectedDotRightPath.moveTo(nextCenterX, dotBottomY)

    // semi circle to the top center
    rectF[nextCenterX - dotRadius, dotTopY, nextCenterX + dotRadius] = dotBottomY
    unselectedDotRightPath.arcTo(rectF, 90f, -180f, true)

    // cubic to the left middle
    endX1 = nextCenterX - dotRadius - joiningFraction * gap
    endY1 = dotCenterY
    controlX1 = nextCenterX - halfDotRadius
    controlY1 = dotTopY
    controlX2 = endX1
    controlY2 = endY1 - halfDotRadius
    unselectedDotRightPath.cubicTo(
      controlX1, controlY1,
      controlX2, controlY2,
      endX1, endY1
    )

    // cubic back to the bottom center
    endX2 = nextCenterX
    endY2 = dotBottomY
    controlX1 = endX1
    controlY1 = endY1 + halfDotRadius
    controlX2 = endX2 - halfDotRadius
    controlY2 = dotBottomY
    unselectedDotRightPath.cubicTo(
      controlX1, controlY1,
      controlX2, controlY2,
      endX2, endY2
    )
    unselectedDotPath.addPath(unselectedDotRightPath)
  }

  private val retreatingJoinPath: Path
    get() {
      unselectedDotPath.rewind()
      rectF[retreatingJoinX1, dotTopY, retreatingJoinX2] = dotBottomY
      unselectedDotPath.addRoundRect(rectF, dotRadius, dotRadius, Path.Direction.CW)
      return unselectedDotPath
    }

  private fun drawSelected(canvas: Canvas) {
    canvas.drawCircle(selectedDotX, dotCenterY, dotRadius, selectedPaint)
  }

  private fun setSelectedPage(now: Int) {
    if (now == currentPage || dotCenterX == null || dotCenterX!!.size <= now) return
    pageChanging = true
    previousPage = currentPage
    currentPage = now
    val steps = abs(now - previousPage)
    if (steps > 1) {
      if (now > previousPage) {
        for (i in 0 until steps) {
          setJoiningFraction(previousPage + i, 1f)
        }
      } else {
        for (i in -1 downTo -steps + 1) {
          setJoiningFraction(previousPage + i, 1f)
        }
      }
    }

    // create the anim to move the selected dot – this animator will kick off
    // retreat animations when it has moved 75% of the way.
    // The retreat animation in turn will kick of reveal anims when the
    // retreat has passed any dots to be revealed
    moveAnimation = createMoveSelectedAnimator(dotCenterX!![now], previousPage, now, steps)
    moveAnimation!!.start()
  }

  private fun createMoveSelectedAnimator(
    moveTo: Float,
    was: Int,
    now: Int,
    steps: Int
  ): ValueAnimator {

    // create the actual move animator
    val moveSelected = ValueAnimator.ofFloat(selectedDotX, moveTo)

    // also set up a pending retreat anim – this starts when the move is 75% complete
    retreatAnimation = PendingRetreatAnimator(
      was, now, steps,
      if (now > was)
        RightwardStartPredicate(moveTo - (moveTo - selectedDotX) * 0.25f)
      else LeftwardStartPredicate(
        moveTo + (selectedDotX - moveTo) * 0.25f
      )
    )
    retreatAnimation!!.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        resetState()
        pageChanging = false
      }
    })
    moveSelected.addUpdateListener { valueAnimator -> // todo avoid autoboxing
      selectedDotX = valueAnimator.animatedValue as Float
      retreatAnimation!!.startIfNecessary(selectedDotX)
      ViewCompat.postInvalidateOnAnimation(this@CustomPageIndicator)
    }
    moveSelected.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationStart(animation: Animator) {
        // set a flag so that we continue to draw the unselected dot in the target position
        // until the selected dot has finished moving into place
        selectedDotInPosition = false
      }

      override fun onAnimationEnd(animation: Animator) {
        // set a flag when anim finishes so that we don't draw both selected & unselected
        // page dots
        selectedDotInPosition = true
      }
    })
    // slightly delay the start to give the joins a chance to run
    // unless dot isn't in position yet – then don't delay!
    moveSelected.startDelay = if (selectedDotInPosition) animDuration / 4L else 0L
    moveSelected.duration = animDuration * 3L / 4L
    moveSelected.interpolator = interpolator
    return moveSelected
  }

  private fun setJoiningFraction(leftDot: Int, fraction: Float) {
    if (leftDot < joiningFractions.size) {
      joiningFractions[leftDot] = fraction
      ViewCompat.postInvalidateOnAnimation(this)
    }
  }

  private fun clearJoiningFractions() {
    Arrays.fill(joiningFractions, 0f)
    ViewCompat.postInvalidateOnAnimation(this)
  }

  private fun setDotRevealFraction(dot: Int, fraction: Float) {
    if (dot < dotRevealFractions.size) {
      dotRevealFractions[dot] = fraction
    }
    ViewCompat.postInvalidateOnAnimation(this)
  }

  private fun cancelJoiningAnimations() {
    if (joiningAnimationSet != null && joiningAnimationSet.isRunning) {
      joiningAnimationSet.cancel()
    }
  }

  /**
   * A [ValueAnimator] that starts once a given predicate returns true.
   */
  abstract inner class PendingStartAnimator(private var predicate: StartPredicate) :
    ValueAnimator() {
    private var hasStarted = false
    fun startIfNecessary(currentValue: Float) {
      if (!hasStarted && predicate.shouldStart(currentValue)) {
        start()
        hasStarted = true
      }
    }
  }

  inner class PendingRetreatAnimator(was: Int, now: Int, steps: Int, predicate: StartPredicate) :
    PendingStartAnimator(predicate) {
    init {
      duration = animHalfDuration
      interpolator = interpolator

      // work out the start/end values of the retreating join from the direction we're
      // travelling in.  Also look at the current selected dot position, i.e. we're moving on
      // before a prior anim has finished.
      val initialX1 = if (now > was) min(
        dotCenterX!![was],
        selectedDotX
      ) - dotRadius else dotCenterX!![now] - dotRadius
      val finalX1 = if (now > was) dotCenterX!![now] - dotRadius else dotCenterX!![now] - dotRadius
      val initialX2 = if (now > was) dotCenterX!![now] + dotRadius else max(
        dotCenterX!![was], selectedDotX
      ) + dotRadius
      val finalX2 = if (now > was) dotCenterX!![now] + dotRadius else dotCenterX!![now] + dotRadius
      revealAnimations = arrayOfNulls(steps)
      // hold on to the indexes of the dots that will be hidden by the retreat so that
      // we can initialize their revealFraction's i.e. make sure they're hidden while the
      // reveal animation runs
      val dotsToHide = IntArray(steps)
      if (initialX1 != finalX1) { // rightward retreat
        setFloatValues(initialX1, finalX1)
        // create the reveal animations that will run when the retreat passes them
        for (i in 0 until steps) {
          revealAnimations[i] = PendingRevealAnimator(
            was + i,
            RightwardStartPredicate(dotCenterX!![was + i])
          )
          dotsToHide[i] = was + i
        }
        addUpdateListener { valueAnimator -> // todo avoid autoboxing
          retreatingJoinX1 = valueAnimator.animatedValue as Float
          ViewCompat.postInvalidateOnAnimation(this@CustomPageIndicator)
          // start any reveal animations if we've passed them
          for (pendingReveal in revealAnimations) {
            pendingReveal!!.startIfNecessary(retreatingJoinX1)
          }
        }
      } else { // (initialX2 != finalX2) leftward retreat
        setFloatValues(initialX2, finalX2)
        // create the reveal animations that will run when the retreat passes them
        for (i in 0 until steps) {
          revealAnimations[i] = PendingRevealAnimator(
            was - i,
            LeftwardStartPredicate(dotCenterX!![was - i])
          )
          dotsToHide[i] = was - i
        }
        addUpdateListener { valueAnimator -> // todo avoid autoboxing
          retreatingJoinX2 = valueAnimator.animatedValue as Float
          ViewCompat.postInvalidateOnAnimation(this@CustomPageIndicator)
          // start any reveal animations if we've passed them
          for (pendingReveal in revealAnimations) {
            pendingReveal!!.startIfNecessary(retreatingJoinX2)
          }
        }
      }
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
          cancelJoiningAnimations()
          clearJoiningFractions()
          // we need to set this so that the dots are hidden until the reveal anim runs
          for (dot in dotsToHide) {
            setDotRevealFraction(dot, MINIMAL_REVEAL)
          }
          retreatingJoinX1 = initialX1
          retreatingJoinX2 = initialX2
          ViewCompat.postInvalidateOnAnimation(this@CustomPageIndicator)
        }

        override fun onAnimationEnd(animation: Animator) {
          retreatingJoinX1 = INVALID_FRACTION
          retreatingJoinX2 = INVALID_FRACTION
          ViewCompat.postInvalidateOnAnimation(this@CustomPageIndicator)
        }
      })
    }
  }

  /**
   * An Animator that animates a given dot's revealFraction i.e. scales it up
   */
  inner class PendingRevealAnimator(dot: Int, predicate: StartPredicate) :
    PendingStartAnimator(predicate) {
    private val dot: Int

    init {
      setFloatValues(MINIMAL_REVEAL, 1f)
      this.dot = dot
      duration = animHalfDuration
      interpolator = interpolator
      addUpdateListener { valueAnimator -> // todo avoid autoboxing
        setDotRevealFraction(
          this@PendingRevealAnimator.dot,
          valueAnimator.animatedValue as Float
        )
      }
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          setDotRevealFraction(this@PendingRevealAnimator.dot, 0f)
          ViewCompat.postInvalidateOnAnimation(this@CustomPageIndicator)
        }
      })
    }
  }

  /**
   * A predicate used to start an animation when a test passes
   */
  abstract inner class StartPredicate(protected var thresholdValue: Float) {
    abstract fun shouldStart(currentValue: Float): Boolean
  }

  /**
   * A predicate used to start an animation when a given value is greater than a threshold
   */
  inner class RightwardStartPredicate(thresholdValue: Float) : StartPredicate(thresholdValue) {
    override fun shouldStart(currentValue: Float): Boolean = currentValue > thresholdValue
  }

  /**
   * A predicate used to start an animation then a given value is less than a threshold
   */
  inner class LeftwardStartPredicate(thresholdValue: Float) : StartPredicate(thresholdValue) {
    override fun shouldStart(currentValue: Float): Boolean = currentValue < thresholdValue
  }

  public override fun onRestoreInstanceState(state: Parcelable) {
    val savedState = state as SavedState
    super.onRestoreInstanceState(savedState.superState)
    currentPage = savedState.currentPage
    requestLayout()
  }

  public override fun onSaveInstanceState(): Parcelable? {
    val superState = super.onSaveInstanceState()
    val savedState = SavedState(superState)
    savedState.currentPage = currentPage
    return savedState
  }

  internal class SavedState : BaseSavedState {
    var currentPage = 0

    constructor(superState: Parcelable?) : super(superState)
    private constructor(`in`: Parcel) : super(`in`) {
      currentPage = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      super.writeToParcel(dest, flags)
      dest.writeInt(currentPage)
    }

    companion object {
      @JvmField val CREATOR: Parcelable.Creator<SavedState?> =
        object : Parcelable.Creator<SavedState?> {
          override fun createFromParcel(`in`: Parcel): SavedState? = SavedState(`in`)

          override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
  }

  companion object {
    // defaults
    private const val DEFAULT_DOT_SIZE = 8 // dp
    private const val DEFAULT_GAP = 12 // dp
    private const val DEFAULT_ANIM_DURATION = 400 // ms
    private const val DEFAULT_UNSELECTED_COLOUR = -0x7f000001 // 50% white
    private const val DEFAULT_SELECTED_COLOUR = -0x1 // 100% white

    // constants
    private const val INVALID_FRACTION = -1f
    private const val MINIMAL_REVEAL = 0.00001f
  }

  init {
    val density = context.resources.displayMetrics.density.toInt()

    // Load attributes
    val a = getContext().obtainStyledAttributes(
      attrs, R.styleable.CustomPageIndicator, defStyle, 0
    )
    dotDiameter = a.getDimensionPixelSize(
      R.styleable.CustomPageIndicator_ipi_dotDiameter,
      DEFAULT_DOT_SIZE * density
    )
    dotRadius = (dotDiameter / 2).toFloat()
    halfDotRadius = dotRadius / 2
    gap = a.getDimensionPixelSize(
      R.styleable.CustomPageIndicator_ipi_dotGap,
      DEFAULT_GAP * density
    )
    animDuration = a.getInteger(
      R.styleable.CustomPageIndicator_ipi_animationDuration,
      DEFAULT_ANIM_DURATION
    ).toLong()
    animHalfDuration = animDuration / 2
    unselectedColour = a.getColor(
      R.styleable.CustomPageIndicator_ipi_pageIndicatorColor,
      DEFAULT_UNSELECTED_COLOUR
    )
    selectedColour = a.getColor(
      R.styleable.CustomPageIndicator_ipi_currentPageIndicatorColor,
      DEFAULT_SELECTED_COLOUR
    )
    a.recycle()
    unselectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    unselectedPaint.color = unselectedColour
    selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    selectedPaint.color = selectedColour
    interpolator = FastOutSlowInInterpolator()

    // create paths & rect now – reuse & rewind later
    combinedUnselectedPath = Path()
    unselectedDotPath = Path()
    unselectedDotLeftPath = Path()
    unselectedDotRightPath = Path()
    rectF = RectF()
    addOnAttachStateChangeListener(this)
  }
}
