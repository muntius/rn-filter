import React, { PureComponent } from 'react'
import {
  requireNativeComponent,
  NativeModules,
  UIManager,
  findNodeHandle
} from 'react-native'
import PropTypes from 'prop-types'
export default class FilterView extends PureComponent {
  constructor (props) {
    super(props)
    this.saturation = UIManager.getViewManagerConfig(
      'RNFilter'
    ).Commands.setSaturation
    this.contrast = UIManager.getViewManagerConfig(
      'RNFilter'
    ).Commands.setContrast
    this.brightness = UIManager.getViewManagerConfig(
      'RNFilter'
    ).Commands.setBrightness
    this.vignette = UIManager.getViewManagerConfig(
      'RNFilter'
    ).Commands.setVignette
    this.blur = UIManager.getViewManagerConfig('RNFilter').Commands.setBlur
    this.capture = UIManager.getViewManagerConfig('RNFilter').Commands.capture
    this.setSaturation = this._setSaturation.bind(this)
    this.setContrast = this._setContrast.bind(this)
    this.setBrightness = this._setBrightness.bind(this)
    this.setVignette = this._setVignette.bind(this)
    this.setBlur = this._setBlur.bind(this)
    this.takeShot = this._takeShot.bind(this)
    this.reqPromise = null
  }

  componentDidMount () {
    this.props.onRef && this.props.onRef(this)
    this.viewHandle = findNodeHandle(this.filterRef)
  }

  _setSaturation (value) {
    UIManager.dispatchViewManagerCommand(this.viewHandle, this.saturation, [
      value
    ])
  }

  _setBrightness (value) {
    UIManager.dispatchViewManagerCommand(this.viewHandle, this.brightness, [
      value
    ])
  }

  _setContrast (value) {
    UIManager.dispatchViewManagerCommand(this.viewHandle, this.contrast, [
      value
    ])
  }

  _setVignette (value) {
    UIManager.dispatchViewManagerCommand(this.viewHandle, this.vignette, [
      value
    ])
  }

  _setBlur (value) {
    UIManager.dispatchViewManagerCommand(this.viewHandle, this.blur, [value])
  }

  _takeShot (media) {
    const { height, width } = media

    const promise = new Promise((resolve, reject) => {
      this.reqPromise = { resolve, reject }
      UIManager.dispatchViewManagerCommand(this.viewHandle, this.capture, [
        height,
        width
      ])
    }).catch(error => {
      console.log('caught', error)
    })
    return promise
  }

  onDataReturned = ({ nativeEvent: { url } }: {nativeEvent: {url: string}}) => {
    const { resolve, reject } = this.reqPromise
    if (url) {
      resolve(url)
    } else {
      reject('error')
    }
    this.reqPromise = null
  }

  render () {
    return (
      <RNFilter
        style={this.props.style}
        src={this.props.src}
        ref={ref => (this.filterRef = ref)}
        onDataReturned={this.onDataReturned}
      />
    )
  }
}

const RNFilter = requireNativeComponent('RNFilter', FilterView, {
  nativeOnly: { onDataReturned: true }
})
