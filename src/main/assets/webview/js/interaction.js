(function() {

/**
 * You can drag this many pixels past the edge of the chart and still have it
 * be considered a zoom. This makes it easier to zoom to the exact edge of the
 * chart, a fairly common operation.
 */
var DRAG_EDGE_MARGIN = 100;

var context = {
  // Tracks whether the mouse is down right now
  isZooming: false,
  isPanning: false,  // is this drag part of a pan?
  is2DPan: false,    // if so, is that pan 1- or 2-dimensional?
  dragStartX: null, // pixel coordinates
  dragStartY: null, // pixel coordinates
  dragEndX: null, // pixel coordinates
  dragEndY: null, // pixel coordinates
  dragDirection: null,
  prevEndX: null, // pixel coordinates
  prevEndY: null, // pixel coordinates
  prevDragDirection: null,
  cancelNextDblclick: false,  // see comment in Interaction-model.js

  // The value on the left side of the graph when a pan operation starts.
  initialLeftmostDate: null,

  // The number of units each pixel spans. (This won't be valid for log
  // scales)
  xUnitsPerPixel: null,

  // TODO(danvk): update this comment
  // The range in second/value units that the viewport encompasses during a
  // panning operation.
  dateRange: null,

  // Top-left corner of the canvas, in DOM coords
  // TODO(konigsberg): Rename topLeftCanvasX, topLeftCanvasY.
  px: 0,
  py: 0,

  // Values for use with panEdgeFraction, which limit how far outside the
  // graph's data boundaries it can be panned.
  boundedDates: null, // [minDate, maxDate]
  boundedValues: null, // [[minValue, maxValue] ...]

  // We cover iframes during mouse interactions. See comments in
  // dygraph-utils.js for more info on why this is a good idea.
  tarp: new Dygraph.IFrameTarp(),

  // contextB is the same thing as this context object but renamed.
  initializeMouseDown: function(event, g, contextB) {
    // prevents mouse drags from selecting page text.
    if (event.preventDefault) {
      event.preventDefault();  // Firefox, Chrome, etc.
    } else {
      event.returnValue = false;  // IE
      event.cancelBubble = true;
    }

    var canvasPos = Dygraph.findPos(g.canvas_);
    contextB.px = canvasPos.x;
    contextB.py = canvasPos.y;
    contextB.dragStartX = Dygraph.dragGetX_(event, contextB);
    contextB.dragStartY = Dygraph.dragGetY_(event, contextB);
    contextB.cancelNextDblclick = false;
    contextB.tarp.cover();
  },
  destroy: function(gs) {
    var context = this;
    if (context.isZooming || context.isPanning) {
      context.isZooming = false;
      context.dragStartX = null;
      context.dragStartY = null;
    }

    if (context.isPanning) {
      context.isPanning = false;
      context.draggingDate = null;
      context.dateRange = null;
        for (var i=0; i<gs.length; i++) {
        for (var j = 0; j < gs[i].axes_.length; j++) {
          delete gs[i].axes_[j].draggingValue;
          delete gs[i].axes_[j].dragValueRange;
        }
      }
    }

    context.tarp.uncover();
  }
};

/**
 * A collection of functions to facilitate build custom interaction models.
 * @class
 */
Interaction = {};

/**
 * Checks whether the beginning & ending of an event were close enough that it
 * should be considered a click. If it should, dispatch appropriate events.
 * Returns true if the event was treated as a click.
 *
 * @param {Event} event
 * @param {Dygraph} g
 * @param {Object} context
 */
Interaction.maybeTreatMouseOpAsClick = function(event, gs) {
  context.dragEndX = Dygraph.dragGetX_(event, context);
  context.dragEndY = Dygraph.dragGetY_(event, context);
  var regionWidth = Math.abs(context.dragEndX - context.dragStartX);
  var regionHeight = Math.abs(context.dragEndY - context.dragStartY);

  if (regionWidth < 2 && regionHeight < 2 &&
      gs[0].lastx_ !== undefined && gs[0].lastx_ != -1) {
    Interaction.treatMouseOpAsClick(gs, event);
  }

  context.regionWidth = regionWidth;
  context.regionHeight = regionHeight;
};

/**
 * Called in response to an interaction model operation that
 * should start the default panning behavior.
 *
 * It's used in the default callback for "mousedown" operations.
 * Custom interaction model builders can use it to provide the default
 * panning behavior.
 *
 * @param {Event} event the event object which led to the startPan call.
 * @param {Dygraph} g The dygraph on which to act.
 * @param {Object} context The dragging context object (with
 *     dragStartX/dragStartY/etc. properties). This function modifies the
 *     context.
 */
Interaction.startPan = function(event, gs) {
  var i, axis;
  context.isPanning = true;
  var xRange = gs[0].xAxisRange();

  if (gs[0].getOptionForAxis("logscale", "x")) {
    context.initialLeftmostDate = Dygraph.log10(xRange[0]);
    context.dateRange = Dygraph.log10(xRange[1]) - Dygraph.log10(xRange[0]);
  } else {
    context.initialLeftmostDate = xRange[0];    
    context.dateRange = xRange[1] - xRange[0];
  }
  context.xUnitsPerPixel = context.dateRange / (gs[0].plotter_.area.w - 1);

  if (gs[0].getNumericOption("panEdgeFraction")) {
    var maxXPixelsToDraw = gs[0].width_ * gs[0].getNumericOption("panEdgeFraction");
    var xExtremes = gs[0].xAxisExtremes(); // I REALLY WANT TO CALL THIS xTremes!

    var boundedLeftX = gs[0].toDomXCoord(xExtremes[0]) - maxXPixelsToDraw;
    var boundedRightX = gs[0].toDomXCoord(xExtremes[1]) + maxXPixelsToDraw;

    var boundedLeftDate = gs[0].toDataXCoord(boundedLeftX);
    var boundedRightDate = gs[0].toDataXCoord(boundedRightX);
    context.boundedDates = [boundedLeftDate, boundedRightDate];

    var boundedValues = [];
    var maxYPixelsToDraw = gs[0].height_ * gs[0].getNumericOption("panEdgeFraction");

    for (i = 0; i < gs[0].axes_.length; i++) {
      axis = gs[0].axes_[i];
      var yExtremes = axis.extremeRange;

      var boundedTopY = gs[0].toDomYCoord(yExtremes[0], i) + maxYPixelsToDraw;
      var boundedBottomY = gs[0].toDomYCoord(yExtremes[1], i) - maxYPixelsToDraw;

      var boundedTopValue = gs[0].toDataYCoord(boundedTopY, i);
      var boundedBottomValue = gs[0].toDataYCoord(boundedBottomY, i);

      boundedValues[i] = [boundedTopValue, boundedBottomValue];
    }
    context.boundedValues = boundedValues;
  }

  // Record the range of each y-axis at the start of the drag.
  // If any axis has a valueRange or valueWindow, then we want a 2D pan.
  // We can't store data directly in g.axes_, because it does not belong to us
  // and could change out from under us during a pan (say if there's a data
  // update).
  context.is2DPan = false;
  context.axes = [];
  for (i = 0; i < gs[0].axes_.length; i++) {
    axis = gs[0].axes_[i];
    var axis_data = {};
    var yRange = gs[0].yAxisRange(i);
    // TODO(konigsberg): These values should be in |context|.
    // In log scale, initialTopValue, dragValueRange and unitsPerPixel are log scale.
    var logscale = gs[0].attributes_.getForAxis("logscale", i);
    if (logscale) {
      axis_data.initialTopValue = Dygraph.log10(yRange[1]);
      axis_data.dragValueRange = Dygraph.log10(yRange[1]) - Dygraph.log10(yRange[0]);
    } else {
      axis_data.initialTopValue = yRange[1];
      axis_data.dragValueRange = yRange[1] - yRange[0];
    }
    axis_data.unitsPerPixel = axis_data.dragValueRange / (gs[0].plotter_.area.h - 1);
    context.axes.push(axis_data);

    // While calculating axes, set 2dpan.
    if (axis.valueWindow || axis.valueRange) context.is2DPan = true;
  }
};

/**
 * Called in response to an interaction model operation that
 * responds to an event that pans the view.
 *
 * It's used in the default callback for "mousemove" operations.
 * Custom interaction model builders can use it to provide the default
 * panning behavior.
 *
 * @param {Event} event the event object which led to the movePan call.
 * @param {Dygraph} g The dygraph on which to act.
 * @param {Object} context The dragging context object (with
 *     dragStartX/dragStartY/etc. properties). This function modifies the
 *     context.
 */
Interaction.movePan = function(event, gs) {
  context.dragEndX = Dygraph.dragGetX_(event, context);
  context.dragEndY = Dygraph.dragGetY_(event, context);

  var minDate = context.initialLeftmostDate -
    (context.dragEndX - context.dragStartX) * context.xUnitsPerPixel;
  if (context.boundedDates) {
    minDate = Math.max(minDate, context.boundedDates[0]);
  }
  var maxDate = minDate + context.dateRange;
  if (context.boundedDates) {
    if (maxDate > context.boundedDates[1]) {
      // Adjust minDate, and recompute maxDate.
      minDate = minDate - (maxDate - context.boundedDates[1]);
      maxDate = minDate + context.dateRange;
    }
  }

  for (var i=0; i<gs.length; i++) {
    g = gs[i];
    if (g.getOptionForAxis("logscale", "x")) {
      g.dateWindow_ = [ Math.pow(Dygraph.LOG_SCALE, minDate),
                        Math.pow(Dygraph.LOG_SCALE, maxDate) ];
    } else {
      g.dateWindow_ = [minDate, maxDate];    
    }
  }

  // y-axis scaling is automatic unless this is a full 2D pan.
  /*if (context.is2DPan) {

    var pixelsDragged = context.dragEndY - context.dragStartY;

    // Adjust each axis appropriately.
    for (var i = 0; i < g.axes_.length; i++) {
      var axis = g.axes_[i];
      var axis_data = context.axes[i];
      var unitsDragged = pixelsDragged * axis_data.unitsPerPixel;

      var boundedValue = context.boundedValues ? context.boundedValues[i] : null;

      // In log scale, maxValue and minValue are the logs of those values.
      var maxValue = axis_data.initialTopValue + unitsDragged;
      if (boundedValue) {
        maxValue = Math.min(maxValue, boundedValue[1]);
      }
      var minValue = maxValue - axis_data.dragValueRange;
      if (boundedValue) {
        if (minValue < boundedValue[0]) {
          // Adjust maxValue, and recompute minValue.
          maxValue = maxValue - (minValue - boundedValue[0]);
          minValue = maxValue - axis_data.dragValueRange;
        }
      }
      if (g.attributes_.getForAxis("logscale", i)) {
        axis.valueWindow = [ Math.pow(Dygraph.LOG_SCALE, minValue),
                             Math.pow(Dygraph.LOG_SCALE, maxValue) ];
      } else {
        axis.valueWindow = [ minValue, maxValue ];
      }
    }
  }*/

  for (var i=0; i<gs.length; i++) {
    gs[i].drawGraph_(false);
  }
};

/**
 * Called in response to an interaction model operation that
 * responds to an event that ends panning.
 *
 * It's used in the default callback for "mouseup" operations.
 * Custom interaction model builders can use it to provide the default
 * panning behavior.
 *
 * @param {Event} event the event object which led to the endPan call.
 * @param {Dygraph} g The dygraph on which to act.
 * @param {Object} context The dragging context object (with
 *     dragStartX/dragStartY/etc. properties). This function modifies the
 *     context.
 */
Interaction.endPan = Interaction.maybeTreatMouseOpAsClick;

/**
 * Called in response to an interaction model operation that
 * responds to an event that starts zooming.
 *
 * It's used in the default callback for "mousedown" operations.
 * Custom interaction model builders can use it to provide the default
 * zooming behavior.
 *
 * @param {Event} event the event object which led to the startZoom call.
 * @param {Dygraph} g The dygraph on which to act.
 * @param {Object} context The dragging context object (with
 *     dragStartX/dragStartY/etc. properties). This function modifies the
 *     context.
 */
Interaction.startZoom = function(event, gs) {
  context.isZooming = true;
  context.zoomMoved = false;
};

/**
 * Called in response to an interaction model operation that
 * responds to an event that defines zoom boundaries.
 *
 * It's used in the default callback for "mousemove" operations.
 * Custom interaction model builders can use it to provide the default
 * zooming behavior.
 *
 * @param {Event} event the event object which led to the moveZoom call.
 * @param {Dygraph} g The dygraph on which to act.
 * @param {Object} context The dragging context object (with
 *     dragStartX/dragStartY/etc. properties). This function modifies the
 *     context.
 */
Interaction.moveZoom = function(event, gs) {
  context.zoomMoved = true;
  context.dragEndX = Dygraph.dragGetX_(event, context);
  context.dragEndY = Dygraph.dragGetY_(event, context);

  var xDelta = Math.abs(context.dragStartX - context.dragEndX);
  var yDelta = Math.abs(context.dragStartY - context.dragEndY);

  // drag direction threshold for y axis is twice as large as x axis
  //context.dragDirection = (xDelta < yDelta / 2) ? Dygraph.VERTICAL : Dygraph.HORIZONTAL;
  context.dragDirection = Dygraph.HORIZONTAL;

  for (var i=0; i<gs.length; i++) {
    gs[i].drawZoomRect_(
        context.dragDirection,
        context.dragStartX,
        context.dragEndX,
        context.dragStartY,
        context.dragEndY,
        context.prevDragDirection,
        context.prevEndX,
        context.prevEndY);
  }

  context.prevEndX = context.dragEndX;
  context.prevEndY = context.dragEndY;
  context.prevDragDirection = context.dragDirection;
};

/**
 * TODO(danvk): move this logic into dygraph.js
 * @param {Dygraph} g
 * @param {Event} event
 * @param {Object} context
 */
Interaction.treatMouseOpAsClick = function(gs, event) {
  var clickCallback = gs[0].getFunctionOption('clickCallback');
  var pointClickCallback = gs[0].getFunctionOption('pointClickCallback');

  var selectedPoint = null;

  // Find out if the click occurs on a point.
  var closestIdx = -1;
  var closestDistance = Number.MAX_VALUE;

  // check if the click was on a particular point.
  for (var i = 0; i < gs[0].selPoints_.length; i++) {
    var p = gs[0].selPoints_[i];
    var distance = Math.pow(p.canvasx - context.dragEndX, 2) +
                   Math.pow(p.canvasy - context.dragEndY, 2);
    if (!isNaN(distance) &&
        (closestIdx == -1 || distance < closestDistance)) {
      closestDistance = distance;
      closestIdx = i;
    }
  }

  // Allow any click within two pixels of the dot.
  var radius = gs[0].getNumericOption('highlightCircleSize') + 2;
  if (closestDistance <= radius * radius) {
    selectedPoint = gs[0].selPoints_[closestIdx];
  }

  if (selectedPoint) {
    var e = {
      cancelable: true,
      point: selectedPoint,
      canvasx: context.dragEndX,
      canvasy: context.dragEndY
    };
    var defaultPrevented = gs[0].cascadeEvents_('pointClick', e);
    if (defaultPrevented) {
      // Note: this also prevents click / clickCallback from firing.
      return;
    }
    if (pointClickCallback) {
      pointClickCallback.call(gs[0], event, selectedPoint);
    }
  }

  var e = {
    cancelable: true,
    xval: gs[0].lastx_,  // closest point by x value
    pts: gs[0].selPoints_,
    canvasx: context.dragEndX,
    canvasy: context.dragEndY
  };
  if (!gs[0].cascadeEvents_('click', e)) {
    if (clickCallback) {
      // TODO(danvk): pass along more info about the points, e.g. 'x'
      clickCallback.call(gs[0], event, gs[0].lastx_, gs[0].selPoints_);
    }
  }
};

/**
 * Called in response to an interaction model operation that
 * responds to an event that performs a zoom based on previously defined
 * bounds..
 *
 * It's used in the default callback for "mouseup" operations.
 * Custom interaction model builders can use it to provide the default
 * zooming behavior.
 *
 * @param {Event} event the event object which led to the endZoom call.
 * @param {Dygraph} g The dygraph on which to end the zoom.
 * @param {Object} context The dragging context object (with
 *     dragStartX/dragStartY/etc. properties). This function modifies the
 *     context.
 */
Interaction.endZoom = function(event, gs) {
  for (var i=0; i<gs.length; i++)
    gs[i].clearZoomRect_();
  context.isZooming = false;
  Interaction.maybeTreatMouseOpAsClick(event, gs);

  // The zoom rectangle is visibly clipped to the plot area, so its behavior
  // should be as well.
  // See http://code.google.com/p/dygraphs/issues/detail?id=280
  var plotArea = gs[0].getArea();
  if (context.regionWidth >= 10 &&
      context.dragDirection == Dygraph.HORIZONTAL) {
    var left = Math.min(context.dragStartX, context.dragEndX),
        right = Math.max(context.dragStartX, context.dragEndX);
    left = Math.max(left, plotArea.x);
    right = Math.min(right, plotArea.x + plotArea.w);
    if (left < right) {
      for (var i=0; i<gs.length; i++)
        gs[i].doZoomX_(left, right);
    }
    context.cancelNextDblclick = true;
  } else if (context.regionHeight >= 10 &&
             context.dragDirection == Dygraph.VERTICAL) {
    var top = Math.min(context.dragStartY, context.dragEndY),
        bottom = Math.max(context.dragStartY, context.dragEndY);
    top = Math.max(top, plotArea.y);
    bottom = Math.min(bottom, plotArea.y + plotArea.h);
    if (top < bottom) {
      for (var i=0; i<gs.length; i++)
        gs[i].doZoomY_(top, bottom);
    }
    context.cancelNextDblclick = true;
  }
  context.dragStartX = null;
  context.dragStartY = null;
};

/**
 * @private
 */
Interaction.startTouch = function(event, gs) {
  event.preventDefault();  // touch browsers are all nice.
  if (event.touches.length > 1) {
    // If the user ever puts two fingers down, it's not a double tap.
    context.startTimeForDoubleTapMs = null;
  }

  var touches = [];
  for (var i = 0; i < event.touches.length; i++) {
    var t = event.touches[i];
    // we dispense with 'dragGetX_' because all touchBrowsers support pageX
    touches.push({
      pageX: t.pageX,
      pageY: t.pageY,
      dataX: gs[0].toDataXCoord(t.pageX),
      //dataY: gs[0].toDataYCoord(t.pageY)
      // identifier: t.identifier
    });
  }
  context.initialTouches = touches;

  if (touches.length == 1) {
    // This is just a swipe.
    context.initialPinchCenter = touches[0];
  } else if (touches.length >= 2) {
    // It's become a pinch!
    // In case there are 3+ touches, we ignore all but the "first" two.

    // only screen coordinates can be averaged (data coords could be log scale).
    context.initialPinchCenter = {
      pageX: 0.5 * (touches[0].pageX + touches[1].pageX),
      pageY: 0.5 * (touches[0].pageY + touches[1].pageY),

      // TODO(danvk): remove
      dataX: 0.5 * (touches[0].dataX + touches[1].dataX),
      //dataY: 0.5 * (touches[0].dataY + touches[1].dataY)
    };
  }

  // save the full x & y ranges.
  context.initialRange = {
    x: gs[0].xAxisRange(),
    y: gs[0].yAxisRange()
  };
};

/**
 * @private
 */
Interaction.moveTouch = function(event, gs) {
  // If the tap moves, then it's definitely not part of a double-tap.
  context.startTimeForDoubleTapMs = null;

  var i, touches = [];
  for (i = 0; i < event.touches.length; i++) {
    var t = event.touches[i];
    touches.push({
      pageX: t.pageX,
      pageY: t.pageY
    });
  }
  var initialTouches = context.initialTouches;

  var c_now;

  // old and new centers.
  var c_init = context.initialPinchCenter;
  if (touches.length == 1) {
    c_now = touches[0];
  } else {
    c_now = {
      pageX: 0.5 * (touches[0].pageX + touches[1].pageX),
      pageY: 0.5 * (touches[0].pageY + touches[1].pageY)
    };
  }

  // this is the "swipe" component
  // we toss it out for now, but could use it in the future.
  var swipe = {
    pageX: c_now.pageX - c_init.pageX,
    pageY: c_now.pageY - c_init.pageY
  };
  var dataWidth = context.initialRange.x[1] - context.initialRange.x[0];
  var dataHeight = context.initialRange.y[0] - context.initialRange.y[1];
  swipe.dataX = (swipe.pageX / gs[0].plotter_.area.w) * dataWidth;
  var xScale;

  // The residual bits are usually split into scale & rotate bits, but we split
  // them into x-scale and y-scale bits.
  if (touches.length == 1) {
    xScale = 1.0;
  } else if (touches.length >= 2) {
    var initHalfWidth = (initialTouches[1].pageX - c_init.pageX);
    xScale = (touches[1].pageX - c_now.pageX) / initHalfWidth;
  }

  // Clip scaling to [1/8, 8] to prevent too much blowup.
  xScale = Math.min(8, Math.max(0.125, xScale));

  var didZoom = false;
  if (Math.abs(swipe.pageY) > Math.abs(swipe.pageX)) {
    window.scrollBy(0, -swipe.pageY);
  }
  else if (Math.abs(swipe.pageX) > 50) {
    for (var i=0; i<gs.length; i++) {
      gs[i].dateWindow_ = [
        c_init.dataX - swipe.dataX + (context.initialRange.x[0] - c_init.dataX) / xScale,
        c_init.dataX - swipe.dataX + (context.initialRange.x[1] - c_init.dataX) / xScale
      ];
    }
    didZoom = true;
    for (var i=0; i<gs.length; i++)
      gs[i].drawGraph_(false);
  }

  // We only call zoomCallback on zooms, not pans, to mirror desktop behavior.
  if (didZoom && touches.length > 1 && gs[0].getFunctionOption('zoomCallback')) {
    var viewWindow = gs[0].xAxisRange();
    gs[0].getFunctionOption("zoomCallback").call(gs[0], viewWindow[0], viewWindow[1], gs[0].yAxisRanges());
  }
};

/**
 * @private
 */
Interaction.endTouch = function(event, gs) {
  if (event.touches.length !== 0) {
    // this is effectively a "reset"
    Interaction.startTouch(event, gs);
  } else if (event.changedTouches.length == 1) {
    // Could be part of a "double tap"
    // The heuristic here is that it's a double-tap if the two touchend events
    // occur within 500ms and within a 50x50 pixel box.
    var now = new Date().getTime();
    var t = event.changedTouches[0];
    if (context.startTimeForDoubleTapMs &&
        now - context.startTimeForDoubleTapMs < 500 &&
        context.doubleTapX && Math.abs(context.doubleTapX - t.screenX) < 50 &&
        context.doubleTapY && Math.abs(context.doubleTapY - t.screenY) < 50) {
      for (var i=0; i<gs.length; i++)
        gs[i].resetZoom();
    } else {
      context.startTimeForDoubleTapMs = now;
      context.doubleTapX = t.screenX;
      context.doubleTapY = t.screenY;
    }
  }
};

// Determine the distance from x to [left, right].
var distanceFromInterval = function(x, left, right) {
  if (x < left) {
    return left - x;
  } else if (x > right) {
    return x - right;
  } else {
    return 0;
  }
};

/**
 * Returns the number of pixels by which the event happens from the nearest
 * edge of the chart. For events in the interior of the chart, this returns zero.
 */
var distanceFromChart = function(event, g) {
  var chartPos = Dygraph.findPos(g.canvas_);
  var box = {
    left: chartPos.x,
    right: chartPos.x + g.canvas_.offsetWidth,
    top: chartPos.y,
    bottom: chartPos.y + g.canvas_.offsetHeight
  };

  var pt = {
    x: Dygraph.pageX(event),
    y: Dygraph.pageY(event)
  };

  var dx = distanceFromInterval(pt.x, box.left, box.right),
      dy = distanceFromInterval(pt.y, box.top, box.bottom);
  //return Math.max(dx, dy);
  return dx;
};

/**
 * Default interation model for dygraphs. You can refer to specific elements of
 * this when constructing your own interaction model, e.g.:
 * g.updateOptions( {
 *   interactionModel: {
 *     mousedown: Dygraph.defaultInteractionModel.mousedown
 *   }
 * } );
 */
Interaction.defaultModel = function(gs) {
  return {
  // Track the beginning of drag events
  mousedown: function(event, old_g, old_context) {
    // Right-click should not initiate a zoom.
    if (event.button && event.button == 2) return;

    context.initializeMouseDown(event, gs[0], context);

    if (event.altKey || event.shiftKey) {
      Interaction.startZoom(event, gs);
    } else {
      Interaction.startPan(event, gs);
    }
  },

  mousemove: function(event, old_g, old_context) {
    if (context.isZooming) {
  	// When the mouse moves >200px from the chart edge, cancel the zoom.
  	var d = distanceFromChart(event, gs[0]);
  	if (d < DRAG_EDGE_MARGIN) {
  	  Interaction.moveZoom(event, gs);
  	} else {
  	  if (context.dragEndX !== null) {
  		context.dragEndX = null;
  		context.dragEndY = null;
  		for (var i=0; i<gs.length; i++)
  		  gs[i].clearZoomRect_();
  	  }
  	}
    } else if (context.isPanning) {
  	Interaction.movePan(event, gs);
    }
  },

  mouseup: function(event, old_g, old_context) {
    if (context.isZooming) {
  	if (context.dragEndX !== null) {
  	  Interaction.endZoom(event, gs);
  	} else {
  	  Interaction.maybeTreatMouseOpAsClick(event, gs);
  	}
    } else if (context.isPanning) {
  	Interaction.endPan(event, gs);
    }
    context.destroy(gs);
  },
  willDestroyContextMyself: true,

  touchstart: function(event, old_g, old_context) {
    Interaction.startTouch(event, gs);
  },
  touchmove: function(event, old_g, old_context) {
    Interaction.moveTouch(event, gs);
  },
  touchend: function(event, old_g, old_context) {
    Interaction.endTouch(event, gs);
  },

  // Disable zooming out if panning.
  dblclick: function(event, old_g, old_context) {
    if (context.cancelNextDblclick) {
      context.cancelNextDblclick = false;
      return;
    }

    // Give plugins a chance to grab this event.
    var e = {
      canvasx: context.dragEndX,
      canvasy: context.dragEndY
    };
    if (gs[0].cascadeEvents_('dblclick', e)) {
      return;
    }

    if (event.altKey || event.shiftKey) {
      return;
    }
    for (var i=0; i<gs.length; i++)
      gs[i].resetZoom();
  }
}};

})();
