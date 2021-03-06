package rpgboss.editor.resourceselector

import scala.swing._
import rpgboss.editor._
import rpgboss.model.resource._
import rpgboss.model._
import scala.swing.event.MouseClicked
import scala.swing.event.EditDone
import com.typesafe.scalalogging.slf4j.LazyLogging
import rpgboss.editor.misc.MapLocPanel
import rpgboss.editor.uibase._
import rpgboss.editor.imageset.selector.AnimationImageFrameSelector
import rpgboss.lib.Utils

class StringSpecSelectDialog[M, MT](
  owner: Window,
  sm: StateMaster,
  initialSelectionOpt: Option[String],
  allowNone: Boolean,
  metaResource: MetaResource[M, MT],
  onSuccessF: (Option[String]) => Unit)
  extends ResourceSelectDialog(
    owner,
    sm,
    initialSelectionOpt,
    allowNone,
    metaResource) {
  override def specToResourceName(spec: String): String = spec
  override def newRcNameToSpec(name: String, prevSpec: Option[String]): String =
    name

  override def onSuccess(result: Option[String]) = onSuccessF(result)
}

class AnimationImageSelectDialog(
  owner: Window,
  sm: StateMaster,
  initialSelectionOpt: Option[AnimationVisual],
  onSuccessF: (Option[AnimationVisual]) => Unit)
  extends ResourceSelectDialog(
    owner,
    sm,
    initialSelectionOpt,
    false,
    AnimationImage) {
  override def specToResourceName(spec: AnimationVisual): String = 
    spec.animationImage
  override def newRcNameToSpec(
    name: String, 
    prevSpecOpt: Option[AnimationVisual]): AnimationVisual = {
    
    val newSpec = Utils.deepCopy(prevSpecOpt.getOrElse(AnimationVisual()))
    val newAnimationImage = AnimationImage.readFromDisk(sm.getProj, name)
    
    val maxFrameIndex = newAnimationImage.xTiles * newAnimationImage.yTiles - 1
    
    newSpec.animationImage = name
    newSpec.start.frameIndex =
      math.min(newSpec.start.frameIndex, maxFrameIndex)
    newSpec.end.frameIndex = 
      math.min(newSpec.end.frameIndex, maxFrameIndex)
    
    newSpec
  }
  
  override def rightPaneFor(
    selection: AnimationVisual,
    updateF: AnimationVisual => Unit) = {
    val animationImage = 
      AnimationImage.readFromDisk(sm.getProj, selection.animationImage)
    new AnimationImageFrameSelector(animationImage, selection, updateF)
  }
  
  override def onSuccess(result: Option[AnimationVisual]) = onSuccessF(result)
}

class BattleBackgroundSelectDialog(
  owner: Window,
  sm: StateMaster,
  initialSelectionOpt: Option[String],
  onSuccessF: (Option[String]) => Unit)
  extends StringSpecSelectDialog(
    owner,
    sm,
    initialSelectionOpt,
    false,
    BattleBackground,
    onSuccessF) {
  override def rightPaneFor(selection: String, unused: String => Unit) = {
    val img = BattleBackground.readFromDisk(sm.getProj, selection)
    new ImagePanel(img.img) with DisposableComponent
  }
}

class PictureSelectDialog(
  owner: Window,
  sm: StateMaster,
  initialSelectionOpt: Option[String],
  onSuccessF: (Option[String]) => Unit)
  extends StringSpecSelectDialog(
    owner,
    sm,
    initialSelectionOpt,
    false,
    Picture,
    onSuccessF) {
  override def rightPaneFor(selection: String, unused: String => Unit) = {
    val img = Picture.readFromDisk(sm.getProj, selection)
    new ImagePanel(img.img) with DisposableComponent
  }
}

class WindowskinSelectDialog(
  owner: Window,
  sm: StateMaster,
  initialSelectionOpt: Option[String],
  onSuccessF: (Option[String]) => Unit)
  extends StringSpecSelectDialog(
    owner,
    sm,
    initialSelectionOpt,
    false,
    Windowskin,
    onSuccessF) {
  override def rightPaneFor(selection: String, unused: String => Unit) = {
    val img = Windowskin.readFromDisk(sm.getProj, selection)
    new ImagePanel(img.img) with DisposableComponent
  }
}

class MapLocSelectDialog(
  owner: Window,
  sm: StateMaster,
  selectMapOnly: Boolean,
  initialLoc: MapLoc,
  onSuccessF: MapLoc => Unit)
  extends StdDialog(owner, "Select Map")
  with LazyLogging {

  val locPanel = new MapLocPanel(this, sm, initialLoc, selectMapOnly)

  def okFunc(): Unit = {
    onSuccessF(locPanel.loc)
    close()
  }

  contents = new DesignGridPanel {
    row().grid().add(locPanel)
    addButtons(okBtn, cancelBtn)
  }
}

abstract class BrowseField[SpecType](
  owner: Window,
  sm: StateMaster,
  initial: Option[SpecType],
  onUpdate: Option[SpecType] => Unit)
  extends BoxPanel(Orientation.Horizontal) with LazyLogging {

  var model = initial

  val textField = new TextField {
    editable = false
    enabled = true
  }

  def modelToString(m: SpecType): String = m.toString

  def updateWidgets() =
    textField.text = model.map(modelToString).getOrElse("<None>")

  updateWidgets()

  def doBrowse()

  val browseBtn = new Button(Action("...") {
    doBrowse()
    logger.debug("Post-browse button onUpdate")
    updateWidgets()
    onUpdate(model)
  })

  listenTo(textField.mouse.clicks)

  reactions += {
    case MouseClicked(`textField`, _, _, _, _) =>
      browseBtn.action.apply()
  }

  contents += textField
  contents += browseBtn
}

abstract class StringBrowseField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends BrowseField[String](
    owner,
    sm,
    if(initial.isEmpty()) None else Some(initial),
    result => onUpdate(result.getOrElse("")))

class WindowskinField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  override def doBrowse() = {
    val diag = new WindowskinSelectDialog(
      owner, sm, model, model = _)
    diag.open()
  }
}

class BattleBackgroundField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  override def doBrowse() = {
    val diag = new BattleBackgroundSelectDialog(owner, sm, model, model = _)
    diag.open()
  }
}

class PictureField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  override def doBrowse() = {
    val diag = new PictureSelectDialog(owner, sm, model, model = _)
    diag.open()
  }
}

class MsgfontField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  def doBrowse() = {
    val diag = new StringSpecSelectDialog(
      owner, sm, model,
      false, Msgfont,
      model = _)
    diag.open()
  }
}

class MapField(
  owner: Window,
  sm: StateMaster,
  initial: String,
  onUpdate: String => Unit)
  extends StringBrowseField(owner, sm, initial, onUpdate) {
  override def modelToString(m: String) =
    sm.getMap(m).map(_.displayId).getOrElse("[None]")

  def doBrowse() = {
    val diag = new MapLocSelectDialog(
      owner,
      sm,
      true /* selectMapOnly */,
      model.map(mapName => MapLoc(mapName, -1, -1)).get,
      loc => model = Some(loc.map))
    diag.open()
  }
}