/**
 * @providesModule CameraViewAndroid
 */
import React, {
  Component,
  PropTypes,
} from 'react'
import {
  requireNativeComponent,
  UIManager,
  DeviceEventEmitter,
} from 'react-native'

class CameraView extends Component {
  constructor(props, context) {
    super(props, context)
    this.onPictureTaken = this.onPictureTaken.bind(this)
    this.onChange = this.onChange.bind(this)
    this._setRef = this._setRef.bind(this)
    this._sub = undefined
    this._ref = undefined
  }

  componentWillMount() {
    this._sub = DeviceEventEmitter.addListener(
      'cameraResult',
      this.onPictureTaken
    )
  }

  componentWillUnmount() {
    if (this._sub) {
      this._sub.remove()
    }
  }

  onChange(event) {
    const { onBarCodeRead } = this.props

    if (onBarCodeRead) {
      const { type, data } = event.nativeEvent
      onBarCodeRead({ type, data })
    }
  }

  takePicture() {
    UIManager.dispatchViewManagerCommand(
      this._getCameraLayoutHandle(),
      UIManager.RNCameraView.Commands.takePicture,
      null
    )
  }

  _setRef(ref) {
    this._ref = ref
  }

  _getCameraLayoutHandle() {
    return React.findNodeHandle(this._ref)
  }

  onPictureTaken(event) {
    const { onPictureTaken } = this.props

    if (onPictureTaken) {
      const { type, message } = event
      onPictureTaken({ type, message })
    }
  }

  render() {
    return (
      <RNCameraView
        {...this.props}
        ref={this._setRef}
        onChange={this.onChange}
      />
    );
  }

  takePicture(opts) {
    UIManager.dispatchViewManagerCommand(
      this._getCameraLayoutHandle(),
      UIManager.RNCameraView.Commands.takePicture,
      [opts]
    );
  }

  _getCameraLayoutHandle() {
    return React.findNodeHandle(this.refs[RN_CAMERA_REF]);
  }
}

CameraView.propTypes = {
  autoFocus: PropTypes.bool,
  torchMode: PropTypes.string,
  type: PropTypes.string,
  pictureOrientation: PropTypes.string,
  onLayout: PropTypes.bool,
  onBarCodeRead: PropTypes.func,
  onPictureTaken: PropTypes.func,
  viewFinderDisplay: PropTypes.bool,
  viewFinderBackgroundColor: PropTypes.string,
  viewFinderBorderColor: PropTypes.string,
  viewFinderBorderWidth: PropTypes.number,
  viewFinderBorderLength: PropTypes.number,
  viewFinderDrawLaser: PropTypes.bool,
  viewFinderLaserColor: PropTypes.string,
  rotation: PropTypes.number,
  scaleX: PropTypes.number,
  scaleY: PropTypes.number,
  translateX: PropTypes.number,
  translateY: PropTypes.number,
  importantForAccessibility: PropTypes.string,
  accessibilityLabel: PropTypes.string,
  testID: PropTypes.string,
  renderToHardwareTextureAndroid: PropTypes.string,
}

const RNCameraView = requireNativeComponent('RNCameraView', CameraView, {
  nativeOnly: {
    onChange: true,
    accessibilityLiveRegion: 'none',
    accessibilityComponentType: 'button',
  },
})

/**
 * Use module.exports for back-compat
 */
module.exports = CameraView
