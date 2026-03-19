import { Pressable, GestureResponderEvent } from 'react-native'

type MusesIconButtonProps = {
  icon?: React.ReactNode
  onPress?: (event: GestureResponderEvent) => void
}

const MusesIconButton: React.FC<MusesIconButtonProps> = ({ icon, onPress }) => {
  return <Pressable onPress={onPress}>{icon && icon}</Pressable>
}

export default MusesIconButton
